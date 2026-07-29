#!/usr/bin/env python3
"""
注册码生成器
运行: python3 generate_code.py <设备ID>
"""
import sys
import base64
import hashlib
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives import hashes, serialization

def load_private_key(key_path="keys/private_key.pem"):
    """加载私钥"""
    with open(key_path, "rb") as f:
        return serialization.load_pem_private_key(f.read(), password=None)

def generate_code(device_id: str, private_key) -> str:
    """
    用私钥加密设备ID，生成注册码
    """
    # 使用 OAEP padding 进行加密
    ciphertext = private_key.encrypt(
        device_id.encode('utf-8'),
        padding.OAEP(
            mgf=padding.MGF1(algorithm=hashes.SHA256()),
            algorithm=hashes.SHA256(),
            label=None
        )
    )
    
    # Base64 编码
    encoded = base64.b64encode(ciphertext).decode('utf-8')
    
    # 格式化为 XXXX-XXXX-XXXX-XXXX 格式
    # 移除 padding 字符
    encoded = encoded.rstrip('=')
    
    # 分组
    groups = []
    for i in range(0, len(encoded), 4):
        groups.append(encoded[i:i+4])
    
    return '-'.join(groups[:4])  # 取前16个字符，分成4组

def format_code(code: str) -> str:
    """格式化注册码"""
    # 确保每组4个字符
    parts = code.split('-')
    if len(parts) >= 4:
        return '-'.join(parts[:4])
    return code

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("用法: python3 generate_code.py <设备ID>")
        print("示例: python3 generate_code.py a1b2c3d4e5f6")
        sys.exit(1)
    
    device_id = sys.argv[1]
    
    try:
        private_key = load_private_key()
        code = generate_code(device_id, private_key)
        print(f"\n📱 设备ID: {device_id}")
        print(f"🔑 注册码: {code}\n")
    except FileNotFoundError:
        print("❌ 错误: 未找到私钥文件 keys/private_key.pem")
        print("   请先运行: python3 keygen.py")
        sys.exit(1)
