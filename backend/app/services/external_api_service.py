"""
外部食品数据库API服务

此模块用于调用薄荷健康(Boohee) API查询商品信息
通过条形码扫描功能获取食品营养数据
"""
import hashlib
import time
import json
from typing import Optional, Dict, Any
from datetime import datetime, timedelta
import httpx
import threading
from app.config_external_api import (
    BOOHEE_API_URL,
    BOOHEE_APP_ID,
    BOOHEE_APP_KEY,
    BOOHEE_ACCOUNTS,
    BOOHEE_CURRENT_ACCOUNT_INDEX,
    BOOHEE_TOKEN_CACHE_TIME,
    EXTERNAL_API_TIMEOUT,
    EXTERNAL_API_ENABLED
)


# Token缓存（每个账号独立缓存）
_token_cache: Dict[int, Dict[str, Any]] = {}

# 当前账号索引（线程安全）
_current_account_index_lock = threading.Lock()
_current_account_index = BOOHEE_CURRENT_ACCOUNT_INDEX


def _get_current_account() -> Dict[str, str]:
    """获取当前使用的账号配置"""
    global _current_account_index
    with _current_account_index_lock:
        if not BOOHEE_ACCOUNTS:
            # 如果没有配置账号，返回默认值
            return {"app_id": BOOHEE_APP_ID, "app_key": BOOHEE_APP_KEY}
        return BOOHEE_ACCOUNTS[_current_account_index]


def _switch_to_next_account():
    """切换到下一个账号（如果到了最后一个，循环回第一个）"""
    global _current_account_index
    with _current_account_index_lock:
        if not BOOHEE_ACCOUNTS:
            return
        _current_account_index = (_current_account_index + 1) % len(BOOHEE_ACCOUNTS)
        # 清除当前账号的token缓存
        if _current_account_index in _token_cache:
            del _token_cache[_current_account_index]


def _clear_token_cache(account_index: Optional[int] = None):
    """清除token缓存"""
    global _token_cache
    if account_index is not None:
        if account_index in _token_cache:
            del _token_cache[account_index]
    else:
        _token_cache.clear()


def _generate_signature(params: Dict[str, Any], app_key: str) -> str:
    """
    生成API请求签名（根据官方文档）
    
    薄荷API签名算法：
    1. 将所有请求参数（除sign外，app_key不参与运算）按key名进行正序排序
    2. 循环对每个键值进行拼接：key1value1key2value2（没有等号和&）
    3. 将app_key拼接在前后：app_key + 拼接字符串 + app_key
    4. 对整个字符串进行MD5加密，返回32位小写MD5值
    
    Args:
        params: 请求参数字典
        app_key: 应用密钥
    
    Returns:
        签名字符串（32位小写MD5）
    """
    # 排除sign参数和app_key参数
    filtered_params = {k: v for k, v in params.items() if k not in ['sign', 'app_key']}
    
    # 按key排序
    sorted_params = sorted(filtered_params.items())
    
    # 拼接字符串：key1value1key2value2（没有等号和&）
    # 对于数组类型（如foods），需要序列化为JSON字符串
    sign_parts = []
    for k, v in sorted_params:
        if isinstance(v, (list, dict)):
            # 数组或字典需要序列化为JSON字符串（不包含空格）
            value_str = json.dumps(v, separators=(',', ':'), ensure_ascii=False)
        else:
            value_str = str(v)
        sign_parts.append(f"{k}{value_str}")
    
    sign_str = ''.join(sign_parts)
    
    # 将app_key拼接在前后
    sign_str = f"{app_key}{sign_str}{app_key}"
    
    # MD5加密，返回32位小写MD5值
    sign = hashlib.md5(sign_str.encode('utf-8')).hexdigest().lower()
    
    return sign


async def _get_access_token(account_index: Optional[int] = None) -> Optional[str]:
    """
    获取薄荷API的Access Token
    
    Token会被缓存，避免频繁请求
    
    Args:
        account_index: 账号索引，如果为None则使用当前账号
    
    Returns:
        Access Token字符串，获取失败返回None
    """
    global _token_cache
    
    # 确定使用的账号索引
    if account_index is None:
        with _current_account_index_lock:
            account_index = _current_account_index
    
    # 检查缓存
    if account_index in _token_cache:
        cache = _token_cache[account_index]
        if cache.get('expires_at') > datetime.now():
            return cache.get('token')
    
    if not EXTERNAL_API_ENABLED:
        return None
    
    # 获取账号配置
    if account_index is not None and account_index < len(BOOHEE_ACCOUNTS):
        account = BOOHEE_ACCOUNTS[account_index]
        app_id = account["app_id"]
        app_key = account["app_key"]
    else:
        account = _get_current_account()
        app_id = account["app_id"]
        app_key = account["app_key"]
    
    endpoint = '/api/v2/access_tokens'
    params = {
        'app_id': app_id,
        'timestamp': int(time.time())
    }
    
    try:
        # 生成签名
        params['sign'] = _generate_signature(params, app_key)
        
        url = f"{BOOHEE_API_URL}{endpoint}"
        
        # 请求Token
        async with httpx.AsyncClient(timeout=EXTERNAL_API_TIMEOUT) as client:
            response = await client.post(url, data=params)
            
            try:
                result = response.json()
            except Exception:
                return None
            
            if response.status_code == 200:
                # 检查是否是业务失败
                if result.get('success') == 0:
                    return None
                
                # 检查是否有access_token
                token = result.get('access_token')
                if token:
                    # 缓存Token（默认1个月，约2592000秒）
                    _token_cache[account_index] = {
                        'token': token,
                        'expires_at': datetime.now() + timedelta(seconds=2592000 - 60)
                    }
                    return token
                else:
                    return None
            else:
                # HTTP错误，尝试解析错误信息
                if result.get('error'):
                    error_info = result.get('error', {})
                    return None
                else:
                    return None
                
    except httpx.HTTPStatusError:
        return None
    except httpx.RequestError:
        return None
    except Exception:
        return None
    
    return None


async def _call_boohee_api(endpoint: str, params: Dict[str, Any], method: str = 'GET', account_index: Optional[int] = None) -> Optional[Dict[str, Any]]:
    """
    调用薄荷API
    
    Args:
        endpoint: API端点路径
        params: 请求参数字典
        method: 请求方法，GET或POST，默认GET
        account_index: 账号索引，如果为None则使用当前账号
    
    Returns:
        API响应数据，失败返回None
    """
    if not EXTERNAL_API_ENABLED:
        return None
    
    # 确定使用的账号索引
    if account_index is None:
        with _current_account_index_lock:
            account_index = _current_account_index
    
    # 获取账号配置
    if account_index is not None and account_index < len(BOOHEE_ACCOUNTS):
        account = BOOHEE_ACCOUNTS[account_index]
        app_id = account["app_id"]
        app_key = account["app_key"]
    else:
        account = _get_current_account()
        app_id = account["app_id"]
        app_key = account["app_key"]
    
    try:
        # 获取Access Token
        token = await _get_access_token(account_index)
        if not token:
            return None
        
        # 添加公共参数
        timestamp = int(time.time())
        request_params = {
            'app_id': app_id,
            'timestamp': timestamp,
            **params
        }
        
        # 生成签名（AccessToken不参与签名计算）
        request_params['sign'] = _generate_signature(request_params, app_key)
        
        url = f"{BOOHEE_API_URL}{endpoint}"
        
        # 发送请求（AccessToken通过Header传递）
        async with httpx.AsyncClient(timeout=EXTERNAL_API_TIMEOUT, follow_redirects=True) as client:
            try:
                if method.upper() == 'GET':
                    response = await client.get(
                        url,
                        params=request_params,
                        headers={'AccessToken': token}
                    )
                else:
                    # POST请求：参数放在body中
                    # 如果参数中包含数组（如foods），需要作为 JSON 传递
                    if 'foods' in request_params:
                        # foods 是数组，需要作为 JSON 传递
                        # 所有参数（包括 foods, app_id, timestamp, sign）都应该在 POST body 中
                        # 使用 JSON 格式发送所有参数
                        response = await client.post(
                            url,
                            json=request_params,
                            headers={
                                'AccessToken': token,
                                'Content-Type': 'application/json'
                            }
                        )
                    else:
                        # 普通 POST 请求，使用表单数据
                        response = await client.post(
                            url,
                            data=request_params,
                            headers={'AccessToken': token}
                        )
            except Exception:
                return None
            
            try:
                result = response.json()
            except Exception:
                return None
            
            # 根据文档，响应格式可能是：
            # 1. 业务失败: {success: 0, message: 'xxxx'} (HTTP 200)
            # 2. 错误: {error: {code: 1001, message: 'xxxx'}} (HTTP 4xx/5xx)
            # 3. 成功: 标准json数据
            
            if response.status_code == 200:
                # 检查是否是业务失败
                if result.get('success') == 0:
                    return None
                
                # 成功返回数据
                return result
            else:
                # HTTP错误
                if result.get('error'):
                    error_info = result.get('error', {})
                    return None
                else:
                    return None
                
    except httpx.HTTPStatusError as e:
        try:
            e.response.json()
        except:
            pass
        return None
    except httpx.RequestError:
        return None
    except Exception:
        return None
    
    return None


def _parse_nutrition_from_ingredients(ingredients_data: Dict[str, Any], serving_size: float) -> Dict[str, Any]:
    """
    从营养信息接口返回的数据中解析营养信息
    
    返回格式与测试脚本中的格式完全一致：
    {
        "calory": [...],           # 热量信息数组
        "base_ingredients": [...], # 三大营养素数组
        "vitamin": [...],          # 维生素数组
        "mineral": [...],          # 矿物质数组
        "amino_acid": [...],       # 氨基酸数组
        "other_ingredients": [...] # 其它成分数组
    }
    
    Args:
        ingredients_data: 营养信息接口返回的数据
        serving_size: 份量大小（克）
    
    Returns:
        完整的营养信息字典，格式与测试脚本一致
    """
    # 直接返回完整的营养信息结构，与测试脚本格式一致
    return {
        "calory": ingredients_data.get('calory', []),
        "base_ingredients": ingredients_data.get('base_ingredients', []),
        "vitamin": ingredients_data.get('vitamin', []),
        "mineral": ingredients_data.get('mineral', []),
        "amino_acid": ingredients_data.get('amino_acid', []),
        "other_ingredients": ingredients_data.get('other_ingredients', [])
    }


def _extract_basic_nutrition(nutrition_data: Dict[str, Any]) -> Dict[str, Any]:
    """
    从完整营养信息中提取基础营养数据（用于兼容现有的NutritionData模型）
    
    Args:
        nutrition_data: 完整的营养信息字典
    
    Returns:
        基础营养信息字典（calories, protein, carbohydrates, fat, fiber, sugar, sodium）
    """
    nutrition = {
        "calories": 0.0,
        "protein": 0.0,
        "carbohydrates": 0.0,
        "fat": 0.0,
        "fiber": None,
        "sugar": None,
        "sodium": None
    }
    
    # 解析热量信息
    if 'calory' in nutrition_data:
        for item in nutrition_data['calory']:
            name_en = item.get('name_en', '')
            value = float(item.get('value', 0) or 0)
            if name_en == 'total_calory':
                nutrition['calories'] = value
    
    # 解析三大营养素
    if 'base_ingredients' in nutrition_data:
        for ingredient in nutrition_data['base_ingredients']:
            name_en = ingredient.get('name_en', '')
            value = float(ingredient.get('value', 0) or 0)
            
            if name_en == 'protein':
                nutrition['protein'] = value
            elif name_en == 'carbohydrate':
                nutrition['carbohydrates'] = value
            elif name_en == 'fat':
                nutrition['fat'] = value
            
            # 解析子项（如糖、膳食纤维、钠等）
            items = ingredient.get('items', [])
            for item in items:
                item_name_en = item.get('name_en', '')
                item_value = float(item.get('value', 0) or 0)
                
                if item_name_en == 'sugar':
                    nutrition['sugar'] = item_value
                elif item_name_en == 'fiber_dietary':
                    nutrition['fiber'] = item_value
    
    # 解析矿物质中的钠
    if 'mineral' in nutrition_data:
        for mineral in nutrition_data['mineral']:
            name = mineral.get('name', '')
            value = float(mineral.get('value', 0) or 0)
            unit = mineral.get('unit_name', '')
            
            if name == '钠':
                # 钠的单位可能是g或mg，需要统一转换为mg
                if unit == 'g':
                    nutrition['sodium'] = value * 1000  # 转换为mg
                elif unit == 'mg':
                    nutrition['sodium'] = value
    
    return nutrition


async def _get_food_ingredients(food_code: str, weight: float = 100.0) -> Optional[Dict[str, Any]]:
    """
    获取食物的详细营养信息
    
    Args:
        food_code: 食物代码（从条形码搜索获取）
        weight: 食物重量（克），默认100克
    
    Returns:
        营养信息字典，失败返回None
    """
    if not EXTERNAL_API_ENABLED:
        return None
    
    try:
        endpoint = '/api/v2/foods/ingredients'
        params = {
            'foods': [
                {
                    'code': food_code,
                    'weight': weight
                }
            ]
        }
        
        result = await _call_boohee_api(endpoint, params, method='POST')
        
        if not result:
            return None
        
        # 检查是否有错误
        if result.get('error'):
            return None
        
        # 成功返回营养信息
        return result
        
    except Exception:
        return None


def _convert_boohee_food_to_standard(boohee_food: Dict[str, Any], barcode: str, nutrition_data: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
    """
    将薄荷API返回的食品数据转换为标准格式
    
    Args:
        boohee_food: 薄荷API返回的食品数据（从条形码搜索获取）
        barcode: 条形码
        nutrition_data: 营养信息数据（从营养信息接口获取，可选）
    
    Returns:
        标准格式的食品信息字典
    """
    # 提取基本信息
    food_name = boohee_food.get('name', '')
    
    # 从名称中提取品牌（如果名称包含品牌信息，如"统一 阿萨姆奶茶"）
    brand = None
    if ' ' in food_name:
        parts = food_name.split(' ', 1)
        if len(parts[0]) <= 20:  # 品牌名通常较短
            brand = parts[0]
    
    # 提取图片URL
    image_url = boohee_food.get('thumb_image_url')
    
    # 提取单位信息，获取默认单位
    units = boohee_food.get('units', [])
    serving_size = 100  # 默认100g
    serving_unit = "克"
    
    if units and len(units) > 0:
        # 使用第一个单位，或者查找is_default为True的单位
        default_unit = next((u for u in units if u.get('is_default')), units[0])
        if default_unit:
            weight = default_unit.get('weight')
            if weight:
                try:
                    serving_size = float(weight)
                    serving_unit = default_unit.get('unit_name', '克')
                except (ValueError, TypeError):
                    pass
    
    # 提取卡路里（每100g）
    calory_per_100g = float(boohee_food.get('calory', 0) or 0)
    
    # 计算每份的卡路里
    if serving_size == 100:
        calories_per_serving = calory_per_100g
    else:
        # 按比例计算
        calories_per_serving = (calory_per_100g / 100.0) * serving_size
    
    # 解析营养信息
    if nutrition_data:
        # 保存完整的营养信息（与测试脚本格式一致）
        full_nutrition = _parse_nutrition_from_ingredients(nutrition_data, serving_size)
        # 提取基础营养信息（用于兼容现有的NutritionData模型）
        basic_nutrition = _extract_basic_nutrition(full_nutrition)
        # 如果营养信息中没有卡路里，使用条形码搜索返回的卡路里
        if basic_nutrition['calories'] == 0:
            basic_nutrition['calories'] = calories_per_serving
        nutrition_per_serving = basic_nutrition
    else:
        # 如果没有营养信息，只使用条形码搜索返回的卡路里
        nutrition_per_serving = {
            "calories": calories_per_serving,
            "protein": 0.0,
            "carbohydrates": 0.0,
            "fat": 0.0,
            "fiber": None,
            "sugar": None,
            "sodium": None
        }
        full_nutrition = None
    
    result = {
        "name": food_name,
        "brand": brand,
        "category": None,  # 条形码搜索不返回分类
        "serving_size": serving_size,
        "serving_unit": serving_unit,
        "nutrition_per_serving": nutrition_per_serving,
        "barcode": barcode,
        "image_url": image_url
    }
    
    # 如果有完整营养信息，添加到结果中（与测试脚本格式一致）
    if full_nutrition:
        result["full_nutrition"] = full_nutrition
    
    return result


async def _convert_boohee_search_food(
    boohee_food: Dict[str, Any],
    fetch_full_nutrition: bool = True,
) -> Dict[str, Any]:
    """将薄荷食物搜索结果转换为标准结构"""
    code = boohee_food.get('code')
    raw_weight = boohee_food.get('weight')
    try:
        weight = float(raw_weight) if raw_weight is not None else 100.0
    except (TypeError, ValueError):
        weight = 100.0

    raw_calory = boohee_food.get('calory')
    try:
        calory = float(raw_calory) if raw_calory is not None else 0.0
    except (TypeError, ValueError):
        calory = 0.0

    nutrition_per_serving = {
        "calories": calory,
        "protein": 0.0,
        "carbohydrates": 0.0,
        "fat": 0.0,
        "fiber": None,
        "sugar": None,
        "sodium": None,
    }
    full_nutrition: Optional[Dict[str, Any]] = None

    if fetch_full_nutrition and code:
        ingredients_raw = await _get_food_ingredients(code, weight=weight)
        if ingredients_raw:
            full_nutrition = _parse_nutrition_from_ingredients(ingredients_raw, weight)
            nutrition_per_serving = _extract_basic_nutrition(full_nutrition)
            if nutrition_per_serving.get('calories', 0) in (0, 0.0):
                nutrition_per_serving['calories'] = calory

    return {
        "source": "boohee",
        "boohee_id": boohee_food.get('id'),
        "boohee_code": code,
        "food_id": None,
        "code": code,
        "name": boohee_food.get('name'),
        "weight": weight,
        "weight_unit": "克",
        "calory": calory,
        "image_url": boohee_food.get('thumb_image_url'),
        "is_liquid": boohee_food.get('is_liquid'),
        "health_light": boohee_food.get('health_light'),
        "brand": boohee_food.get('brand'),
        "barcode": boohee_food.get('barcode'),
        "nutrition_per_serving": nutrition_per_serving,
        "full_nutrition": full_nutrition,
    }


async def query_food_by_barcode(barcode: str) -> Optional[Dict[str, Any]]:
    """
    根据条形码查询食品信息（调用薄荷API）
    
    Args:
        barcode: 商品条形码
    
    Returns:
        食品信息字典，包含以下字段：
        {
            "name": "食品名称",
            "brand": "品牌",
            "category": "分类",
            "serving_size": 100,  # 标准份量
            "serving_unit": "克",
            "nutrition_per_serving": {
                "calories": 0,
                "protein": 0,
                "carbohydrates": 0,
                "fat": 0,
                "fiber": 0,
                "sugar": 0,
                "sodium": 0
            },
            "barcode": "条形码",
            "image_url": "图片URL（可选）"
        }
        
        如果未找到，返回 None
        
    注意：此函数会自动调用营养信息接口获取完整的营养数据（蛋白质、碳水化合物、脂肪、纤维、糖、钠等）。
    如果营养信息接口调用失败，则只返回条形码搜索提供的基本信息和卡路里。
    """
    if not EXTERNAL_API_ENABLED:
        return None
    
    # 验证条形码格式
    if not await validate_barcode(barcode):
        return None
    
    try:
        # 调用薄荷API条形码搜索接口
        endpoint = '/api/v1/foods/barcode'
        params = {'barcode': barcode}
        
        result = await _call_boohee_api(endpoint, params, method='GET')
        
        if not result:
            return None
        
        # 根据文档，成功返回: {"success":1, "foods":[...]}
        # 失败返回: {"success":0, "message":"找不到对应的食物"}
        if result.get('success') == 1 and result.get('foods'):
            foods = result.get('foods', [])
            if len(foods) > 0:
                # 取第一个商品
                boohee_food = foods[0]
                food_code = boohee_food.get('code')
                
                # 获取单位信息，确定份量大小
                units = boohee_food.get('units', [])
                serving_size = 100  # 默认100g
                if units and len(units) > 0:
                    default_unit = next((u for u in units if u.get('is_default')), units[0])
                    if default_unit:
                        weight = default_unit.get('weight')
                        if weight:
                            try:
                                serving_size = float(weight)
                            except (ValueError, TypeError):
                                pass
                
                # 尝试获取详细营养信息
                nutrition_data = None
                if food_code:
                    nutrition_data = await _get_food_ingredients(food_code, weight=serving_size)
                
                # 转换为标准格式
                return _convert_boohee_food_to_standard(boohee_food, barcode, nutrition_data)
        
        return None
        
    except Exception:
        # 静默失败
        return None


def _is_account_limit_reached(api_result: Dict[str, Any], converted_foods: list) -> bool:
    """
    检测账号是否达到限制
    
    判断标准：
    1. 返回空结果（total_pages=0且foods为空）
    2. 或者所有转换后的结果都是"source": "local"而没有"source": "boohee"
    
    Args:
        api_result: API返回的原始结果
        converted_foods: 转换后的食物列表
    
    Returns:
        如果达到限制返回True，否则返回False
    """
    # 检查是否为空结果
    total_pages = api_result.get('total_pages', 0)
    api_foods = api_result.get('foods', [])
    
    if total_pages == 0 and len(api_foods) == 0:
        return True
    
    # 检查转换后的结果是否都是local而没有boohee
    if len(converted_foods) > 0:
        has_boohee = False
        for food in converted_foods:
            source = food.get('source', 'local')
            if source == 'boohee':
                has_boohee = True
                break
        
        # 如果没有boohee结果，说明账号达到限制
        if not has_boohee:
            return True
    
    return False


async def search_foods(
    keyword: str,
    page: int = 1,
    include_full_nutrition: bool = True,
) -> Dict[str, Any]:
    """
    根据关键字调用薄荷API搜索食物
    
    如果检测到当前账号达到限制，会自动切换到下一个账号并重试。
    如果所有账号都达到限制，返回空结果。
    """
    if not EXTERNAL_API_ENABLED:
        return {"page": page, "total_pages": 0, "foods": []}

    keyword = (keyword or "").strip()
    if not keyword:
        return {"page": page, "total_pages": 0, "foods": []}

    # 记录起始账号索引，避免无限循环
    with _current_account_index_lock:
        start_account_index = _current_account_index
        max_attempts = len(BOOHEE_ACCOUNTS) if BOOHEE_ACCOUNTS else 1
    
    for attempt in range(max_attempts):
        try:
            endpoint = '/api/v1/foods/search'
            params: Dict[str, Any] = {
                'q': keyword,
                'page': page,
            }

            # 使用当前账号索引调用API（线程安全获取）
            with _current_account_index_lock:
                current_index = _current_account_index
            result = await _call_boohee_api(endpoint, params, method='GET', account_index=current_index)

            if not result:
                # API调用失败，尝试下一个账号
                _switch_to_next_account()
                continue

            # 转换数据格式
            foods_data = []
            for boohee_food in result.get('foods', []):
                converted = await _convert_boohee_search_food(
                    boohee_food,
                    fetch_full_nutrition=include_full_nutrition,
                )
                foods_data.append(converted)
            
            # 检查是否达到限制（在转换后检查）
            if _is_account_limit_reached(result, foods_data):
                # 达到限制，切换到下一个账号
                _clear_token_cache(current_index)
                _switch_to_next_account()
                
                # 如果已经尝试了所有账号，返回空结果
                if attempt == max_attempts - 1:
                    return {"page": page, "total_pages": 0, "foods": []}
                
                # 继续尝试下一个账号
                continue

            return {
                "page": result.get('page', page),
                "total_pages": result.get('total_pages', 0),
                "foods": foods_data,
            }

        except Exception:
            # 发生异常，尝试下一个账号
            _switch_to_next_account()
            if attempt == max_attempts - 1:
                return {"page": page, "total_pages": 0, "foods": []}
    
    # 所有账号都尝试失败，返回空结果
    return {"page": page, "total_pages": 0, "foods": []}


async def get_food_by_boohee_id(
    boohee_id: int,
    include_full_nutrition: bool = True,
) -> Optional[Dict[str, Any]]:
    """根据薄荷健康的 boohee_id 获取食物详情"""
    if not EXTERNAL_API_ENABLED:
        return None

    if boohee_id is None:
        return None

    try:
        endpoint = f"/api/v2/foods/{boohee_id}"
        result = await _call_boohee_api(endpoint, {}, method='GET')

        if not result:
            return None

        if isinstance(result, dict):
            if isinstance(result.get('food'), dict):
                food_data = result.get('food')
            else:
                food_data = result
        else:
            return None

        normalized: Dict[str, Any] = dict(food_data)
        normalized['id'] = normalized.get('id', boohee_id)
        normalized.setdefault('code', str(normalized.get('code') or normalized['id']))

        for candidate in [
            normalized.get('weight'),
            normalized.get('serving_size'),
            normalized.get('serving_weight'),
        ]:
            if candidate is not None:
                try:
                    normalized['weight'] = float(candidate)
                    break
                except (TypeError, ValueError):
                    continue
        normalized.setdefault('weight', 100.0)

        if normalized.get('calory') is None:
            for candidate in [
                normalized.get('calorie'),
                normalized.get('calories'),
            ]:
                if candidate is not None:
                    try:
                        normalized['calory'] = float(candidate)
                        break
                    except (TypeError, ValueError):
                        continue

        return await _convert_boohee_search_food(
            normalized,
            fetch_full_nutrition=include_full_nutrition,
        )

    except Exception:
        return None


async def validate_barcode(barcode: str) -> bool:
    """
    验证条形码格式是否正确
    
    Args:
        barcode: 条形码字符串
    
    Returns:
        是否为有效条形码
    """
    # 基本验证：只包含数字，长度在8-13之间（常见条形码格式）
    if not barcode.isdigit():
        return False
    
    length = len(barcode)
    if length < 8 or length > 14:
        return False
    
    return True


class ExternalAPIError(Exception):
    """外部API调用异常"""
    pass

