"""
生成测试用的饮食和运动记录数据

用于测试饮食分析与智能推荐接口

运行方式：
cd backend
python -m scripts.generate_test_data
"""

import asyncio
from datetime import datetime, timedelta
from motor.motor_asyncio import AsyncIOMotorClient
import sys
from pathlib import Path

# 添加项目根目录到 Python 路径
sys.path.insert(0, str(Path(__file__).parent.parent))

from app.config import settings

# 测试用户邮箱
TEST_USER_EMAIL = "user@example.com"

# 模拟食物数据（会先检查/创建这些食物）
SAMPLE_FOODS = [
    {
        "name": "鸡胸肉（清蒸）",
        "category": "肉类",
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition_per_serving": {
            "calories": 165,
            "protein": 31,
            "carbohydrates": 0,
            "fat": 3.6,
            "fiber": 0,
            "sugar": 0,
            "sodium": 74
        }
    },
    {
        "name": "白米饭",
        "category": "主食",
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition_per_serving": {
            "calories": 130,
            "protein": 2.6,
            "carbohydrates": 28,
            "fat": 0.3,
            "fiber": 0.4,
            "sugar": 0,
            "sodium": 1
        }
    },
    {
        "name": "西兰花（炒）",
        "category": "蔬菜",
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition_per_serving": {
            "calories": 55,
            "protein": 3.7,
            "carbohydrates": 7,
            "fat": 0.6,
            "fiber": 3.3,
            "sugar": 1.4,
            "sodium": 41
        }
    },
    {
        "name": "鸡蛋（煮）",
        "category": "蛋类",
        "serving_size": 50,
        "serving_unit": "克",
        "nutrition_per_serving": {
            "calories": 78,
            "protein": 6.3,
            "carbohydrates": 0.6,
            "fat": 5.3,
            "fiber": 0,
            "sugar": 0.6,
            "sodium": 62
        }
    },
    {
        "name": "牛奶（全脂）",
        "category": "乳制品",
        "serving_size": 250,
        "serving_unit": "毫升",
        "nutrition_per_serving": {
            "calories": 150,
            "protein": 8,
            "carbohydrates": 12,
            "fat": 8,
            "fiber": 0,
            "sugar": 12,
            "sodium": 100
        }
    },
    {
        "name": "苹果",
        "category": "水果",
        "serving_size": 150,
        "serving_unit": "克",
        "nutrition_per_serving": {
            "calories": 78,
            "protein": 0.4,
            "carbohydrates": 21,
            "fat": 0.2,
            "fiber": 3.6,
            "sugar": 15,
            "sodium": 1.5
        }
    },
    {
        "name": "面包（全麦）",
        "category": "主食",
        "serving_size": 50,
        "serving_unit": "克",
        "nutrition_per_serving": {
            "calories": 120,
            "protein": 5,
            "carbohydrates": 22,
            "fat": 1.5,
            "fiber": 3,
            "sugar": 3,
            "sodium": 180
        }
    },
    {
        "name": "豆腐（嫩）",
        "category": "豆制品",
        "serving_size": 100,
        "serving_unit": "克",
        "nutrition_per_serving": {
            "calories": 55,
            "protein": 5,
            "carbohydrates": 2,
            "fat": 3,
            "fiber": 0.3,
            "sugar": 0.5,
            "sodium": 7
        }
    },
    {
        "name": "三明治（鸡肉）",
        "category": "快餐",
        "serving_size": 200,
        "serving_unit": "克",
        "nutrition_per_serving": {
            "calories": 350,
            "protein": 20,
            "carbohydrates": 35,
            "fat": 15,
            "fiber": 2,
            "sugar": 5,
            "sodium": 600
        }
    },
    {
        "name": "沙拉（蔬菜）",
        "category": "蔬菜",
        "serving_size": 150,
        "serving_unit": "克",
        "nutrition_per_serving": {
            "calories": 45,
            "protein": 2,
            "carbohydrates": 8,
            "fat": 0.5,
            "fiber": 3,
            "sugar": 3,
            "sodium": 30
        }
    }
]

# 模拟运动类型
SAMPLE_SPORTS = [
    {"sport_type": "跑步", "METs": 8.0},
    {"sport_type": "快走", "METs": 4.5},
    {"sport_type": "游泳", "METs": 7.0},
    {"sport_type": "骑自行车", "METs": 6.0},
    {"sport_type": "瑜伽", "METs": 3.0},
]

# 7天的饮食计划（每天3餐）
DAILY_MEAL_PLANS = [
    # Day 1 - 蛋白质偏低的一天
    {
        "早餐": [("面包（全麦）", 1.5), ("牛奶（全脂）", 1)],
        "午餐": [("白米饭", 2), ("西兰花（炒）", 1.5)],
        "晚餐": [("白米饭", 1.5), ("豆腐（嫩）", 1.5)],
    },
    # Day 2 - 相对均衡
    {
        "早餐": [("鸡蛋（煮）", 2), ("面包（全麦）", 1), ("牛奶（全脂）", 1)],
        "午餐": [("白米饭", 1.5), ("鸡胸肉（清蒸）", 1.5), ("西兰花（炒）", 1)],
        "晚餐": [("白米饭", 1), ("豆腐（嫩）", 2), ("沙拉（蔬菜）", 1)],
    },
    # Day 3 - 蛋白质充足
    {
        "早餐": [("鸡蛋（煮）", 2), ("牛奶（全脂）", 1)],
        "午餐": [("白米饭", 1.5), ("鸡胸肉（清蒸）", 2)],
        "晚餐": [("三明治（鸡肉）", 1), ("沙拉（蔬菜）", 1.5)],
    },
    # Day 4 - 碳水偏高
    {
        "早餐": [("面包（全麦）", 2), ("苹果", 1)],
        "午餐": [("白米饭", 2.5), ("西兰花（炒）", 1)],
        "晚餐": [("白米饭", 2), ("豆腐（嫩）", 1)],
    },
    # Day 5 - 热量偏低
    {
        "早餐": [("苹果", 1), ("牛奶（全脂）", 0.5)],
        "午餐": [("沙拉（蔬菜）", 2), ("鸡蛋（煮）", 1)],
        "晚餐": [("白米饭", 1), ("西兰花（炒）", 1.5)],
    },
    # Day 6 - 相对均衡
    {
        "早餐": [("鸡蛋（煮）", 2), ("面包（全麦）", 1)],
        "午餐": [("白米饭", 1.5), ("鸡胸肉（清蒸）", 1.5), ("沙拉（蔬菜）", 1)],
        "晚餐": [("三明治（鸡肉）", 1), ("苹果", 1)],
    },
    # Day 7 - 今天
    {
        "早餐": [("牛奶（全脂）", 1), ("面包（全麦）", 1.5)],
        "午餐": [("白米饭", 1.5), ("鸡胸肉（清蒸）", 1)],
        "晚餐": [],  # 今天还没吃晚餐
    },
]

# 7天的运动计划
DAILY_SPORTS_PLANS = [
    # Day 1
    [("跑步", 30)],
    # Day 2
    [("快走", 45)],
    # Day 3
    [("游泳", 40)],
    # Day 4 - 休息日
    [],
    # Day 5
    [("瑜伽", 60)],
    # Day 6
    [("骑自行车", 45), ("快走", 20)],
    # Day 7 - 今天
    [("跑步", 25)],
]


async def get_database():
    """获取数据库连接"""
    client = AsyncIOMotorClient(settings.MONGODB_URL)
    return client[settings.DATABASE_NAME]


async def ensure_foods_exist(db):
    """确保测试食物存在于数据库中"""
    food_ids = {}
    
    for food in SAMPLE_FOODS:
        # 检查食物是否存在
        existing = await db.foods.find_one({"name": food["name"]})
        
        if existing:
            food_ids[food["name"]] = str(existing["_id"])
            print(f"  食物已存在: {food['name']}")
        else:
            # 创建新食物
            food_doc = {
                **food,
                "created_by": "all",  # 所有人可见
                "created_at": datetime.utcnow(),
            }
            result = await db.foods.insert_one(food_doc)
            food_ids[food["name"]] = str(result.inserted_id)
            print(f"  创建食物: {food['name']}")
    
    return food_ids


async def ensure_sports_exist(db):
    """确保测试运动类型存在于数据库中"""
    for sport in SAMPLE_SPORTS:
        existing = await db.sports.find_one({"sport_type": sport["sport_type"]})
        
        if existing:
            print(f"  运动类型已存在: {sport['sport_type']}")
        else:
            sport_doc = {
                **sport,
                "email": settings.DEFAULT_SPORT_EMAIL if hasattr(settings, 'DEFAULT_SPORT_EMAIL') else "admin@example.com",
                "created_at": datetime.utcnow(),
            }
            await db.sports.insert_one(sport_doc)
            print(f"  创建运动类型: {sport['sport_type']}")


async def get_user_weight(db, email):
    """获取用户体重"""
    user = await db.users.find_one({"email": email})
    if user and user.get("weight"):
        return float(user["weight"])
    return 70.0  # 默认体重


async def clear_existing_records(db):
    """清除现有的测试记录"""
    # 删除过去7天的饮食记录
    seven_days_ago = datetime.utcnow() - timedelta(days=7)
    
    result1 = await db.food_records.delete_many({
        "user_email": TEST_USER_EMAIL,
        "recorded_at": {"$gte": seven_days_ago}
    })
    print(f"  删除 {result1.deleted_count} 条饮食记录")
    
    result2 = await db.sports_log.delete_many({
        "email": TEST_USER_EMAIL,
        "created_at": {"$gte": seven_days_ago}
    })
    print(f"  删除 {result2.deleted_count} 条运动记录")


async def create_food_records(db, food_ids):
    """创建7天的饮食记录"""
    today = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
    records_created = 0
    
    for day_offset, meal_plan in enumerate(DAILY_MEAL_PLANS):
        record_date = today - timedelta(days=6 - day_offset)
        
        for meal_type, foods in meal_plan.items():
            if not foods:
                continue
                
            # 设置用餐时间
            if meal_type == "早餐":
                meal_time = record_date.replace(hour=7, minute=30)
            elif meal_type == "午餐":
                meal_time = record_date.replace(hour=12, minute=0)
            else:  # 晚餐
                meal_time = record_date.replace(hour=18, minute=30)
            
            for food_name, serving_amount in foods:
                food_id = food_ids.get(food_name)
                if not food_id:
                    print(f"  警告: 找不到食物 {food_name}")
                    continue
                
                # 获取食物营养信息
                food = None
                for f in SAMPLE_FOODS:
                    if f["name"] == food_name:
                        food = f
                        break
                
                if not food:
                    continue
                
                # 计算实际营养数据
                nutrition = {}
                for key, value in food["nutrition_per_serving"].items():
                    nutrition[key] = round(value * serving_amount, 2)
                
                record = {
                    "user_email": TEST_USER_EMAIL,
                    "food_name": food_name,
                    "food_id": food_id,
                    "serving_amount": serving_amount,
                    "serving_size": food["serving_size"],
                    "serving_unit": food["serving_unit"],
                    "nutrition_data": nutrition,
                    "recorded_at": meal_time,
                    "meal_type": meal_type,
                    "notes": "测试数据",
                }
                
                await db.food_records.insert_one(record)
                records_created += 1
    
    return records_created


async def create_sports_records(db):
    """创建7天的运动记录"""
    today = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
    user_weight = await get_user_weight(db, TEST_USER_EMAIL)
    records_created = 0
    
    for day_offset, sports_plan in enumerate(DAILY_SPORTS_PLANS):
        record_date = today - timedelta(days=6 - day_offset)
        
        for sport_type, duration in sports_plan:
            # 获取运动的 METs
            mets = 5.0  # 默认值
            for s in SAMPLE_SPORTS:
                if s["sport_type"] == sport_type:
                    mets = s["METs"]
                    break
            
            # 计算消耗卡路里: METs * 体重(kg) * 时间(小时)
            calories_burned = round(mets * user_weight * (duration / 60), 2)
            
            # 设置运动时间（下午或晚上）
            sport_time = record_date.replace(hour=17, minute=0)
            
            record = {
                "email": TEST_USER_EMAIL,
                "sport_type": sport_type,
                "duration_time": duration,
                "calories_burned": calories_burned,
                "created_at": sport_time,
            }
            
            await db.sports_log.insert_one(record)
            records_created += 1
    
    return records_created


async def main():
    """主函数"""
    print("=" * 50)
    print("生成测试数据")
    print("=" * 50)
    print(f"目标用户: {TEST_USER_EMAIL}")
    print()
    
    db = await get_database()
    
    # 1. 清除现有记录
    print("1. 清除现有的测试记录...")
    await clear_existing_records(db)
    
    # 2. 确保食物数据存在
    print("\n2. 检查/创建食物数据...")
    food_ids = await ensure_foods_exist(db)
    
    # 3. 确保运动类型存在
    print("\n3. 检查/创建运动类型...")
    await ensure_sports_exist(db)
    
    # 4. 创建饮食记录
    print("\n4. 创建7天饮食记录...")
    food_records = await create_food_records(db, food_ids)
    print(f"  创建了 {food_records} 条饮食记录")
    
    # 5. 创建运动记录
    print("\n5. 创建7天运动记录...")
    sports_records = await create_sports_records(db)
    print(f"  创建了 {sports_records} 条运动记录")
    
    print("\n" + "=" * 50)
    print("测试数据生成完成！")
    print("=" * 50)
    print("\n现在可以测试以下接口：")
    print("  - POST /api/ai/diet/analyze  (饮食分析)")
    print("  - GET  /api/ai/meal/recommend (菜式推荐)")
    print()


if __name__ == "__main__":
    asyncio.run(main())

