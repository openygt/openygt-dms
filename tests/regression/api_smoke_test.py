#!/usr/bin/env python3
"""
V2.0 Enhancement API 冒烟测试脚本
=====================================
工程师F / test/v2-enhancement-regression

用法:
    python api_smoke_test.py [--base-url http://localhost:8080] [--user admin] [--password admin123]

环境变量:
    DMS_BASE_URL    默认 http://localhost:8080
    DMS_USERNAME    默认 admin
    DMS_PASSWORD    默认 admin123

退出码:
    0  全部通过
    1  有失败
"""

import os
import sys
import json
import time
import argparse
import uuid
from datetime import datetime
from typing import Optional

try:
    import requests
except ImportError:
    print("[ERROR] 需要 requests 库: pip install requests")
    sys.exit(1)


class DmsSmokeTester:
    def __init__(self, base_url: str, username: str, password: str):
        self.base_url = base_url.rstrip("/")
        self.username = username
        self.password = password
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})
        self.token: Optional[str] = None
        self.user_id: Optional[int] = None
        self.results: list[dict] = []
        self.passed = 0
        self.failed = 0
        self.blocked = 0

    def log(self, case_id: str, desc: str, status: str, detail: str = ""):
        self.results.append({
            "case_id": case_id,
            "desc": desc,
            "status": status,
            "detail": detail,
            "timestamp": datetime.now().isoformat()
        })
        if status == "PASS":
            self.passed += 1
        elif status == "FAIL":
            self.failed += 1
        elif status == "BLOCK":
            self.blocked += 1
        icon = {"PASS": "✅", "FAIL": "❌", "BLOCK": "⛔"}.get(status, "?")
        print(f"{icon} [{case_id}] {desc} — {status} {detail}")

    def req(self, method: str, path: str, auth: bool = True, **kwargs):
        url = f"{self.base_url}{path}"
        headers = kwargs.pop("headers", {})
        if auth and self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        try:
            resp = self.session.request(method, url, headers=headers, timeout=15, **kwargs)
            return resp
        except requests.RequestException as e:
            return type("obj", (object,), {"status_code": 0, "text": str(e), "json": lambda: {}})()

    # ==================== 用例 ====================

    def smk_auth_01_login_ok(self):
        """SMK-AUTH-01: 正常登录"""
        case = "SMK-AUTH-01"
        resp = self.req("POST", "/api/v1/rbac/auth/login", auth=False,
                        data=json.dumps({"username": self.username, "password": self.password}))
        if resp.status_code == 200:
            body = resp.json()
            if body.get("code") == 200 and body.get("data", {}).get("token"):
                self.token = body["data"]["token"]
                self.user_id = body["data"].get("userId")
                roles = body["data"].get("roles", [])
                self.log(case, "正常登录", "PASS", f"roles={roles}")
                return True
        self.log(case, "正常登录", "FAIL", f"status={resp.status_code}, body={resp.text[:200]}")
        return False

    def smk_auth_02_login_bad_password(self):
        """SMK-AUTH-02: 密码错误"""
        case = "SMK-AUTH-02"
        resp = self.req("POST", "/api/v1/auth/login", auth=False,
                        data=json.dumps({"username": self.username, "password": "wrong"}))
        if resp.status_code in (401, 400, 200):
            body = resp.json()
            if body.get("code") != 200:
                self.log(case, "密码错误拦截", "PASS")
                return True
        self.log(case, "密码错误拦截", "FAIL", f"status={resp.status_code}, body={resp.text[:200]}")
        return False

    def smk_auth_03_no_token(self):
        """SMK-AUTH-03: 无 Token 访问受保护接口"""
        case = "SMK-AUTH-03"
        resp = self.req("GET", "/api/v1/rbac/roles", auth=False)
        if resp.status_code == 401:
            self.log(case, "无Token返回401", "PASS")
            return True
        self.log(case, "无Token返回401", "FAIL", f"status={resp.status_code}")
        return False

    def smk_auth_04_no_permission(self):
        """SMK-AUTH-04: 无权限访问返回403"""
        case = "SMK-AUTH-04"
        # 以低权限用户登录（如果有预置数据），否则跳过
        low_user = "jgy001"
        resp = self.req("POST", "/api/v1/rbac/auth/login", auth=False,
                        data=json.dumps({"username": low_user, "password": self.password}))
        if resp.status_code != 200 or resp.json().get("code") != 200:
            self.log(case, "无权限403校验", "BLOCK", "低权限用户不可用")
            return False
        low_token = resp.json()["data"]["token"]
        resp2 = self.session.get(
            f"{self.base_url}/api/v1/rbac/roles",
            headers={"Authorization": f"Bearer {low_token}", "Content-Type": "application/json"},
            timeout=15
        )
        if resp2.status_code == 403:
            self.log(case, "无权限返回403", "PASS")
            return True
        body = resp2.json()
        if body.get("code") == 40003 or body.get("message", "").find("无操作权限") >= 0:
            self.log(case, "无权限返回403/40003", "PASS")
            return True
        self.log(case, "无权限返回403", "FAIL", f"status={resp2.status_code}, body={resp2.text[:200]}")
        return False

    def smk_rbac_01_five_roles(self):
        """SMK-RBAC-01: 恰好返回 5 个预置角色"""
        case = "SMK-RBAC-01"
        resp = self.req("GET", "/api/v1/rbac/roles")
        if resp.status_code == 200:
            body = resp.json()
            roles = body.get("data", [])
            role_codes = [r.get("roleCode") for r in roles]
            expected = {"ROLE_ADMIN", "ROLE_DIRECTOR", "ROLE_LEADER", "ROLE_WORKER", "ROLE_INSPECTOR"}
            if set(role_codes) == expected:
                self.log(case, f"5角色完整 {role_codes}", "PASS")
                return True
            self.log(case, f"角色不匹配 got={role_codes} expected={expected}", "FAIL")
            return False
        self.log(case, "获取角色失败", "FAIL", f"status={resp.status_code}")
        return False

    def smk_rbac_03_assign_menus(self):
        """SMK-RBAC-03: 分配菜单权限"""
        case = "SMK-RBAC-03"
        # 先取角色列表
        resp = self.req("GET", "/api/v1/rbac/roles")
        if resp.status_code != 200:
            self.log(case, "分配菜单权限", "FAIL", "无法获取角色列表")
            return False
        roles = resp.json().get("data", [])
        if not roles:
            self.log(case, "分配菜单权限", "BLOCK", "无角色数据")
            return False
        role_id = roles[0]["id"]
        # 先取菜单列表
        menus_resp = self.req("GET", "/api/v1/rbac/menus")
        menu_ids = [m["id"] for m in menus_resp.json().get("data", [])[:3]] if menus_resp.status_code == 200 else []
        payload = menu_ids
        resp2 = self.req("POST", f"/api/v1/rbac/roles/{role_id}/menus", data=json.dumps(payload))
        if resp2.status_code == 200 and resp2.json().get("code") == 200:
            self.log(case, "分配菜单权限成功", "PASS")
            return True
        self.log(case, "分配菜单权限", "FAIL", f"status={resp2.status_code}, body={resp2.text[:200]}")
        return False

    def smk_prod_01_create_prescription(self):
        """SMK-PROD-01: 创建处方"""
        case = "SMK-PROD-01"
        payload = {
            "hospitalId": 1,
            "patientName": f" Smoke_{uuid.uuid4().hex[:6]}",
            "medicineList": [
                {"medicineName": "黄芪", "dosage": 30, "unit": "g"}
            ]
        }
        resp = self.req("POST", "/api/prescriptions", data=json.dumps(payload))
        if resp.status_code == 200 and resp.json().get("code") == 200:
            self.prescription_id = resp.json().get("data", {}).get("id")
            self.log(case, "创建处方成功", "PASS", f"id={self.prescription_id}")
            return True
        self.log(case, "创建处方", "FAIL", f"status={resp.status_code}, body={resp.text[:200]}")
        return False

    def smk_inv_01_empty_consume(self):
        """SMK-INV-01: 空明细消耗记录不阻断"""
        case = "SMK-INV-01"
        # 先找一个任务ID，若无可创处方
        task_id = getattr(self, "task_id", None)
        if not task_id:
            # 尝试查询最近任务
            r = self.req("GET", "/api/tasks?page=1&size=1")
            if r.status_code == 200:
                records = r.json().get("data", {}).get("records", [])
                if records:
                    task_id = records[0]["id"]
        if not task_id:
            self.log(case, "空明细消耗记录", "BLOCK", "无可用任务ID")
            return False
        payload = {"taskId": task_id, "medicines": []}
        resp = self.req("POST", "/api/inventory/consume/record", data=json.dumps(payload))
        if resp.status_code == 200 and resp.json().get("code") == 200:
            self.log(case, "空明细消耗记录通过", "PASS")
            return True
        self.log(case, "空明细消耗记录", "FAIL", f"status={resp.status_code}, body={resp.text[:200]}")
        return False

    def smk_eq_01_device_list(self):
        """SMK-EQ-01: 设备列表"""
        case = "SMK-EQ-01"
        resp = self.req("GET", "/api/devices?page=1&size=10")
        if resp.status_code == 200 and resp.json().get("code") == 200:
            self.log(case, "设备列表返回", "PASS")
            return True
        self.log(case, "设备列表", "FAIL", f"status={resp.status_code}")
        return False

    def smk_alm_01_alarm_list(self):
        """SMK-ALM-01: 查询告警日志"""
        case = "SMK-ALM-01"
        resp = self.req("GET", "/api/v1/monitor/alarms?page=1&size=10")
        if resp.status_code == 200 and resp.json().get("code") == 200:
            self.log(case, "告警日志返回", "PASS")
            return True
        # 兼容旧路径
        resp2 = self.req("GET", "/api/alarms?page=1&size=10")
        if resp2.status_code == 200:
            self.log(case, "告警日志返回(兼容路径)", "PASS")
            return True
        self.log(case, "告警日志", "FAIL", f"status={resp.status_code}")
        return False

    def smk_alm_05_no_voice_dial(self):
        """SMK-ALM-05: 无语音拨号路径"""
        case = "SMK-ALM-05"
        # 白盒检查：搜索后端源码中的拨号相关调用（仅做简单启发式）
        keywords = ["dial", "sip", "tts", "voiceAlarm", "phone", "拨打", "拨号"]
        found = []
        src_dirs = ["dms-equipment/src/main/java", "dms-app/src/main/java"]
        for d in src_dirs:
            if not os.path.isdir(d):
                continue
            for root, _, files in os.walk(d):
                for f in files:
                    if f.endswith(".java"):
                        path = os.path.join(root, f)
                        try:
                            with open(path, "r", encoding="utf-8", errors="ignore") as fh:
                                for line in fh:
                                    stripped = line.strip()
                                    if stripped.startswith("//") or stripped.startswith("*") or stripped.startswith("/*") or "log." in line or 'System.out.print' in line:
                                        continue
                                    for kw in keywords:
                                        if kw.lower() in line.lower():
                                            found.append(f"{f}: {kw}")
                                            break
                                    else:
                                        continue
                                    break
                        except Exception:
                            pass
        if not found:
            self.log(case, "未检测到语音拨号路径", "PASS")
            return True
        self.log(case, "检测到疑似语音拨号路径", "FAIL", str(found[:5]))
        return False

    # ==================== 运行器 ====================

    def run(self):
        print(f"\n{'='*60}")
        print(f"DMS V2.0 API Smoke Test")
        print(f"Base URL: {self.base_url}")
        print(f"Time: {datetime.now().isoformat()}")
        print(f"{'='*60}\n")

        # 认证链
        if not self.smk_auth_01_login_ok():
            print("\n[CRITICAL] 登录失败，后续用例中止\n")
            self.smk_auth_02_login_bad_password()
            self.smk_auth_03_no_token()
            self.print_summary()
            return 1

        self.smk_auth_02_login_bad_password()
        self.smk_auth_03_no_token()
        self.smk_auth_04_no_permission()

        # RBAC
        self.smk_rbac_01_five_roles()
        self.smk_rbac_03_assign_menus()

        # 生产 / 消耗
        self.smk_prod_01_create_prescription()
        self.smk_inv_01_empty_consume()

        # 设备 / 告警
        self.smk_eq_01_device_list()
        self.smk_alm_01_alarm_list()
        self.smk_alm_05_no_voice_dial()

        return self.print_summary()

    def print_summary(self):
        print(f"\n{'='*60}")
        print("SUMMARY")
        print(f"{'='*60}")
        print(f"Total : {len(self.results)}")
        print(f"Passed: {self.passed}")
        print(f"Failed: {self.failed}")
        print(f"Blocked:{self.blocked}")
        print(f"{'='*60}")

        # 输出 JSON 报告
        report = {
            "meta": {
                "base_url": self.base_url,
                "username": self.username,
                "timestamp": datetime.now().isoformat(),
                "branch": os.environ.get("GITHUB_REF", "local")
            },
            "summary": {
                "total": len(self.results),
                "passed": self.passed,
                "failed": self.failed,
                "blocked": self.blocked
            },
            "details": self.results
        }
        report_dir = "tests/reports"
        os.makedirs(report_dir, exist_ok=True)
        report_path = os.path.join(report_dir, f"smoke-report-{int(time.time())}.json")
        with open(report_path, "w", encoding="utf-8") as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
        print(f"\nReport saved: {report_path}")

        return 0 if self.failed == 0 and self.blocked == 0 else 1


def main():
    parser = argparse.ArgumentParser(description="DMS V2.0 API Smoke Test")
    parser.add_argument("--base-url", default=os.environ.get("DMS_BASE_URL", "http://localhost:8080"))
    parser.add_argument("--user", default=os.environ.get("DMS_USERNAME", "admin"))
    parser.add_argument("--password", default=os.environ.get("DMS_PASSWORD", "admin123"))
    args = parser.parse_args()

    tester = DmsSmokeTester(args.base_url, args.user, args.password)
    rc = tester.run()
    sys.exit(rc)


if __name__ == "__main__":
    main()
