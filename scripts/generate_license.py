#!/usr/bin/env python3
"""
MedTrace 注册码生成器 (HMAC-SHA256 + NDK保护方案)

方案原理:
- 使用 HMAC-SHA256 签名设备ID
- 密钥仅存在于 Python 脚本和 Android NDK .so 文件中
- 黑客反编译 APK 难以提取 .so 中的密钥
- 一次注册，永久有效

注册码结构:
┌────────────────────────────────────────┐
│       32字符 HMAC-SHA256 签名           │
│    (设备ID 的签名，仅含字母和数字)        │
└────────────────────────────────────────┘
"""
import sys
import os
import hmac
import hashlib

def load_secret_key():
    """从文件加载密钥"""
    key_file = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".secret_key")
    if not os.path.exists(key_file):
        print(f"❌ 错误: 未找到密钥文件 {key_file}")
        print("请确保 .secret_key 文件存在且包含32字节的hex密钥")
        sys.exit(1)
    
    with open(key_file, 'r') as f:
        key_hex = f.read().strip()
    
    # 验证密钥长度
    if len(key_hex) != 64:
        print(f"❌ 错误: 密钥长度不正确，应为64个hex字符（32字节），实际为{len(key_hex)}个")
        sys.exit(1)
    
    return bytes.fromhex(key_hex)

def generate_license(android_id: str) -> str:
    """
    生成注册码
    
    流程:
    1. HMAC-SHA256 签名设备ID
    2. 取前12字节 = 24 hex 字符
    3. 格式化为 XXXX-XXXX-XXXX-XXXX-XXXX-XXXX
    """
    # 加载密钥
    secret_key = load_secret_key()
    
    # HMAC-SHA256 签名
    h = hmac.new(secret_key, android_id.strip().upper().encode('utf-8'), hashlib.sha256)
    
    # 取前12字节 = 24 hex 字符
    hex_str = h.hexdigest()[:24]
    
    # 格式化: XXXX-XXXX-XXXX-XXXX-XXXX-XXXX
    return '-'.join([hex_str[i:i+4].upper() for i in range(0, 24, 4)])

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("用法: python3 generate_license.py <设备ID>")
        print("示例: python3 generate_license.py 3938aef9b5b20910")
        print("\n注意: 密钥从 .secret_key 文件加载，不要泄露此文件！")
        sys.exit(1)
    
    android_id = sys.argv[1]
    code = generate_license(android_id)
    
    print(f"\n📱 设备ID: {android_id}")
    print(f"🔑 注册码: {code}\n")
