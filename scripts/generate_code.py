#!/usr/bin/env python3
"""
注册码生成器 (使用 pycryptodome)
运行: python3 generate_code.py <设备ID>
"""
import sys
import base64
from Crypto.PublicKey import RSA
from Crypto.Cipher import PKCS1_OAEP
from Crypto.Hash import SHA256
from Crypto.Random import get_random_bytes

def load_private_key(key_path="keys/private_key.pem"):
    """加载私钥"""
    with open(key_path, "rb") as f:
        return RSA.import_key(f.read())

def generate_code(device_id: str, private_key) -> str:
    """
    用私钥加密设备ID，生成注册码
    使用 SHA-256 作为哈希算法
    """
    # 创建加密器 - 使用 SHA-256，MGF1 默认使用 SHA-256
    cipher = PKCS1_OAEP.new(private_key, hashAlgo=SHA256)
    
    # 加密设备ID
    ciphertext = cipher.encrypt(device_id.encode('utf-8'))
    
    # Base64 编码
    encoded = base64.b64encode(ciphertext).decode('utf-8')
    
    # 移除 padding 字符
    encoded = encoded.rstrip('=')
    
    # 格式化为 XXXX-XXXX-XXXX-XXXX 格式
    groups = []
    for i in range(0, len(encoded), 4):
        groups.append(encoded[i:i+4])
    
    return '-'.join(groups[:4])  # 取前16个字符，分成4组

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
