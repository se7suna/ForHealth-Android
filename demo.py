# -*- coding: utf-8 -*-
"""
自定义图片分析 - 可以自己输入问题询问图片
"""

import os
from qwen_bailian_api import call_qwen_vl_with_url, call_qwen_vl_with_local_file

# ============================================================
# 在这里设置你的API密钥
# ============================================================
API_KEY = "sk-83b57c8f1845479599839bca6aee0431"  # 修改为你的API密钥
# ============================================================

# ============================================================
# 图片设置（二选一）
# ============================================================
# 方式1: 使用本地图片（填写本地路径）
LOCAL_IMAGE = r"test.jpg"

# 方式2: 使用网络图片（填写URL）
IMAGE_URL = "https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg"

# 选择使用哪种方式: "local" 或 "url"
USE_MODE = "local"  # 改成 "url" 可以使用网络图片
# ============================================================


def main():
    print("=" * 60)
    print("Qwen多模态图片分析 - 自定义提问")
    print("=" * 60)
    
    # 检查API密钥
    if not API_KEY or API_KEY == "your-api-key-here":
        print("\n错误: 请在代码开头设置你的API密钥")
        return
    
    # 显示当前使用的图片
    if USE_MODE == "local":
        if not os.path.exists(LOCAL_IMAGE):
            print(f"\n错误: 本地图片不存在: {LOCAL_IMAGE}")
            return
        print(f"\n当前图片: {LOCAL_IMAGE}")
    else:
        print(f"\n当前图片: {IMAGE_URL}")
    
    print("\n提示: 输入 'quit' 或 'exit' 退出程序")
    print("=" * 60)
    
    while True:
        try:
            # 获取用户输入
            question = input("\n请输入你的问题: ").strip()
            
            # 检查是否退出
            if question.lower() in ['quit', 'exit', '退出', 'q']:
                print("\n再见！")
                break
            
            # 检查是否为空
            if not question:
                print("问题不能为空，请重新输入")
                continue
            
            print("\n正在分析图片...")
            print("-" * 60)
            
            # 根据模式调用不同的API
            if USE_MODE == "local":
                result = call_qwen_vl_with_local_file(
                    image_path=LOCAL_IMAGE,
                    prompt=question,
                    model="qwen-vl-plus",
                    api_key=API_KEY
                )
            else:
                result = call_qwen_vl_with_url(
                    image_url=IMAGE_URL,
                    prompt=question,
                    model="qwen-vl-plus",
                    api_key=API_KEY
                )
            
            print("回答:")
            print(result)
            print("-" * 60)
            
        except KeyboardInterrupt:
            print("\n\n程序被中断，再见！")
            break
        except Exception as e:
            print(f"\n错误: {e}")
            print("请重试或输入 'quit' 退出")


if __name__ == "__main__":
    main()
