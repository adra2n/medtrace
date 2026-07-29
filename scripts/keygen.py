#!/usr/bin/env python3
"""
RSA 密钥对生成器 (使用 pycryptodome)
运行: python3 keygen.py
"""
from Crypto.PublicKey import RSA
import os

def generate_keypair():
    """生成 RSA 密钥对"""
    key = RSA.generate(2048)
    return key

def save_keys(key, output_dir="keys"):
    """保存密钥到文件"""
    os.makedirs(output_dir, exist_ok=True)
    
    # 保存私钥 (PEM格式)
    private_key = key.export_key('PEM')
    with open(os.path.join(output_dir, "private_key.pem"), "wb") as f:
        f.write(private_key)
    
    # 保存公钥 (PEM格式)
    public_key = key.publickey().export_key('PEM')
    with open(os.path.join(output_dir, "public_key.pem"), "wb") as f:
        f.write(public_key)
    
    # 保存公钥 (Android用的字符串格式)
    with open(os.path.join(output_dir, "public_key.txt"), "w") as f:
        f.write(public_key.decode('utf-8'))
    
    print(f"✅ 密钥对已生成到 {output_dir}/")
    print(f"   - private_key.pem (私钥，保密！)")
    print(f"   - public_key.pem (公钥)")
    print(f"   - public_key.txt (公钥字符串，嵌入App用)")

if __name__ == "__main__":
    key = generate_keypair()
    save_keys(key)
