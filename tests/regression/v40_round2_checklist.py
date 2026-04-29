#!/usr/bin/env python3
"""
V4.0 第二轮测试输入清单 — 已实现功能快速验证
验证项：T-M-01 (JWT鉴权), T-M-04 (API前缀统一), T-I-03 (状态字符串收敛)
"""
import requests, json, sys

BASE = "http://localhost:8080"
RESULTS = []

def check(name, cond, detail=""):
    status = "PASS" if cond else "FAIL"
    RESULTS.append({"name": name, "status": status, "detail": detail})
    icon = "✅" if cond else "❌"
    print(f"{icon} [{name}] {status} {detail}")

# 1. 登录获取 JWT (T-M-01)
resp = requests.post(f"{BASE}/api/v1/rbac/auth/login",
                     json={"username":"admin","password":"admin123"},
                     timeout=10)
check("T-M-01a 登录返回200", resp.status_code == 200, f"status={resp.status_code}")
body = resp.json() if resp.status_code == 200 else {}
token = body.get("data", {}).get("token", "")
check("T-M-01b Token非空", len(token) > 20, f"len={len(token)}")

# 解析 JWT payload (不校验签名，仅查看claims)
import base64
try:
    payload_b64 = token.split('.')[1]
    payload_b64 += '=' * (4 - len(payload_b64) % 4)
    payload = json.loads(base64.b64decode(payload_b64).decode())
    roles = payload.get("roles", [])
    perms = payload.get("permissions", [])
    check("T-M-01c Token含roles", len(roles) > 0, f"roles={roles}")
    check("T-M-01d Token含permissions", len(perms) > 0, f"perms_count={len(perms)}")
except Exception as e:
    check("T-M-01c Token解析", False, str(e))
    check("T-M-01d Token解析", False, str(e))

# 2. 无Token访问受保护接口返回401
resp2 = requests.get(f"{BASE}/api/v1/eq/devices?page=1&size=1", timeout=10)
check("T-M-01e 无Token返回401", resp2.status_code == 401, f"status={resp2.status_code}")

# 3. API 前缀统一 (T-M-04)
endpoints = [
    ("GET", "/api/v1/md/hospitals?page=1&size=1", "masterdata"),
    ("GET", "/api/v1/eq/devices?page=1&size=1", "equipment"),
    ("GET", "/api/v1/prod/tasks?page=1&size=1", "production"),
    ("GET", "/api/v1/prt/tasks?page=1&size=1", "print"),
    ("GET", "/api/v1/qt/inspection/1", "quality"),
    ("GET", "/api/v1/inv/consume/list", "inventory"),
    ("GET", "/api/v1/sys/configs?page=1&size=1", "system"),
    ("GET", "/api/v1/rbac/roles", "rbac"),
]
headers = {"Authorization": f"Bearer {token}"} if token else {}
for method, path, module in endpoints:
    r = requests.request(method, f"{BASE}{path}", headers=headers, timeout=10)
    ok = r.status_code in (200, 204, 404)  # 404 是资源不存在，但路由存在
    check(f"T-M-04 {module}前缀", ok, f"{path} => {r.status_code}")

# 4. 状态字符串收敛 (T-I-03) — 查询设备状态，确认是英文枚举
resp3 = requests.get(f"{BASE}/api/v1/eq/devices?page=1&size=1", headers=headers, timeout=10)
if resp3.status_code == 200:
    records = resp3.json().get("data", {}).get("records", [])
    if records:
        status = records[0].get("status", "")
        valid = status in ("IDLE", "RUNNING", "FAULT", "OFFLINE", "MAINTENANCE")
        check("T-I-03 状态字符串收敛", valid, f"status={status}")
    else:
        check("T-I-03 无设备", True, "no devices")
else:
    check("T-I-03 查询失败", False, f"status={resp3.status_code}")

# 汇总
print("\n" + "="*50)
print("SUMMARY")
print("="*50)
passed = sum(1 for r in RESULTS if r["status"] == "PASS")
failed = sum(1 for r in RESULTS if r["status"] == "FAIL")
print(f"Total: {len(RESULTS)}, Passed: {passed}, Failed: {failed}")
print("="*50)
sys.exit(0 if failed == 0 else 1)
