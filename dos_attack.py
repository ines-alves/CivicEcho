#!/usr/bin/env python3

import socket
import threading
import time

target_ip = "192.168.0.20"
target_port = 8090

def attempt_connection():
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(1)
        sock.connect((target_ip, target_port))
        print(f"Connection successful")
        sock.close()
        return True
    except Exception as e:
        print(f"Connection failed: {e}")
        return False

print("Test 1: Normal rate (2 connections/second)")
for i in range(6):
    attempt_connection()
    time.sleep(0.5)

print("\nTest 2: DoS simulation (10 connections instantly)")
threads = []
for i in range(15):
    t = threading.Thread(target=attempt_connection)
    threads.append(t)
    t.start()

for t in threads:
    t.join()

print("\nTest 3: Sustained attack")
success_count = 0
for i in range(100):
    if attempt_connection():
        success_count += 1
    time.sleep(0.1)  # 10 connections/second

print(f"\nTotal successful connections: {success_count}/100")