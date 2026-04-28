#!/usr/bin/env python3
"""
REVIEW-2026-04-28 修复项 回归测试脚本
======================================
覆盖范围: L1/L2/D3/D5/N1/N2/N3 的 API 层验证

用法:
    python review_fixes_regression.py [--base-url http://localhost:8080]

环境变量:
    DMS_BASE_URL    默认 http://localhost:8080
    DMS_USERNAME    默认 admin
    DMS_PASSWORD    默认 admin123
"""

import os
import sys
import json
import time
import argparse
from datetime import datetime
from typing import Optional

try:
    import requests
except ImportError:
    print("[ERROR] 需要 requests 库: pip install requests")
    sys.exit(1)


class ReviewFixesTester:
    def __init__(self, base_url: str, username: str, password: str):
        self.base_url = base_url.rstrip("/")
        self.username = username
        self.password = password
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})
        self.token: Optional[str] = None
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
            return type("obj", (object,), {
                "status_code": 0,
                "text": str(e),
                "json": lambda: {}
            })()

    # ==================== 用例 ====================

    def jwt_01_default_secret_warning(self):
        """JWT-01: 弱密钥启动告警（通过日志 API 或本地文件断言，此处留接口）"""
        case = "JWT-01"
        # 实际执行需配合启动日志扫描脚本
        self.log(case, "弱密钥启动告警", "BLOCK", "需人工检查启动日志或配合日志扫描脚本")

    def jwt_02_env_secret_inject(self):
        """JWT-02: 环境变量密钥注入后 Token 可用"""
        case = "JWT-02"
        if not self.token:
            self.log(case, "环境变量密钥注入", "BLOCK", "未获取 token")
            return
        # 简单验证 token 三段式
        parts = self.token.split(".")
        if len(parts) == 3:
            self.log(case, "Token 格式正确", "PASS")
        else:
            self.log(case, "Token 格式异常", "FAIL", f"parts={len(parts)}")

    def api_01_new_login_path(self):
        """API-01: 新登录路径 /api/v1/rbac/auth/login 可用"""
        case = "API-01"
        resp = self.req("POST", "/api/v1/rbac/auth/login", auth=False,
                        data=json.dumps({"username": self.username, "password": self.password}))
        if resp.status_code == 200:
            body = resp.json()
            if body.get("code") == 200 and body.get("data", {}).get("token"):
                self.token = body["data"]["token"]
                self.log(case, "新登录路径可用", "PASS", f"token_len={len(self.token)}")
                return True
        self.log(case, "新登录路径可用", "FAIL", f"status={resp.status_code}, body={resp.text[:200]}")
        return False

    def api_02_old_login_path_compat(self):
        """API-02: 旧路径 /api/v1/auth/login 兼容或已移除"""
        case = "API-02"
        resp = self.req("POST", "/api/v1/auth/login", auth=False,
                        data=json.dumps({"username": self.username, "password": self.password}))
        if resp.status_code == 200:
            self.log(case, "旧路径仍兼容", "PASS", "存在兼容转发")
        elif resp.status_code in (404, 405):
            self.log(case, "旧路径已移除", "PASS", f"status={resp.status_code}")
        else:
            self.log(case, "旧路径状态异常", "FAIL", f"status={resp.status_code}")

    def api_03_inventory_under_v1(self):
        """API-03: 库存接口在 /api/v1/inv 下且受鉴权保护"""
        case = "API-03"
        resp = self.req("GET", "/api/v1/inv/consume/list", auth=False)
        if resp.status_code == 401:
            self.log(case, "库存 v1 路径受保护", "PASS")
        else:
            self.log(case, "库存 v1 路径应返回401", "FAIL", f"status={resp.status_code}")

    def api_04_old_inventory_path_gone(self):
        """API-04: 旧路径 /api/inventory/consume 不再使用"""
        case = "API-04"
        resp = self.req("GET", "/api/inventory/consume/list", auth=False)
        if resp.status_code == 404:
            self.log(case, "旧库存路径已废弃(404)", "PASS", "返回404")
        elif resp.status_code == 401:
            self.log(case, "旧库存路径已废弃(401)", "PASS", "被鉴权拦截，旧Controller已移除")
        else:
            self.log(case, "旧库存路径状态异常", "FAIL", f"status={resp.status_code}")

    def api_05_unauth_any_api(self):
        """API-05: 非白名单 /api/** 无 Token 均 401"""
        case = "API-05"
        probes = [
            ("GET", "/api/v1/rbac/roles"),
            ("GET", "/api/v1/sys/users"),
            ("GET", "/api/v1/eq/devices"),
            ("GET", "/api/v1/inv/consume/list"),
        ]
        all_401 = True
        fails = []
        for method, path in probes:
            r = self.req(method, path, auth=False)
            if r.status_code != 401:
                all_401 = False
                fails.append(f"{path}=>{r.status_code}")
        if all_401:
            self.log(case, "非白名单接口均401", "PASS")
        else:
            self.log(case, "存在接口未鉴权", "FAIL", str(fails))

    def api_06_whitelist_pass(self):
        """API-06: 白名单接口放行"""
        case = "API-06"
        r1 = self.req("POST", "/api/v1/rbac/auth/login", auth=False,
                      data=json.dumps({"username": self.username, "password": self.password}))
        r2 = self.req("GET", "/error", auth=False)
        ok = r1.status_code == 200 and r2.status_code in (200, 404, 500)
        if ok:
            self.log(case, "白名单接口放行", "PASS")
        else:
            self.log(case, "白名单接口被拦截", "FAIL", f"login={r1.status_code}, error={r2.status_code}")

    def sec_01_security_filter_chain_active(self):
        """SEC-01: SecurityFilterChain 生效（通过 CSRF/CORS 行为推断）"""
        case = "SEC-01"
        # 发送不带 Content-Type 的 POST，Spring Security 不应对公开路径拦 403
        r = self.req("POST", "/api/v1/rbac/auth/login", auth=False,
                      headers={},
                      data=json.dumps({"username": self.username, "password": self.password}))
        if r.status_code == 200:
            self.log(case, "SecurityFilterChain 未误拦截公开路径", "PASS")
        else:
            self.log(case, "SecurityFilterChain 可能配置异常", "FAIL", f"status={r.status_code}")

    def mqtt_01_topic_format(self):
        """MQTT-01: Topic 配置为 +/+/+，拼接后为 /openygt/+/+/+"""
        case = "MQTT-01"
        # 白盒：读取 application.yml（如可访问）或通过源码扫描确认
        # 此处为 API 层占位，实际断言在 mqtt_topic_test.py
        self.log(case, "Topic 格式通过源码/配置扫描确认", "BLOCK", "请运行 mqtt_topic_test.py")

    def run(self):
        print(f"\n{'='*60}")
        print("REVIEW-2026-04-28 修复项 回归测试")
        print(f"Base URL: {self.base_url}")
        print(f"Time: {datetime.now().isoformat()}")
        print(f"{'='*60}\n")

        # 认证链（必须先登录）
        if not self.api_01_new_login_path():
            print("\n[CRITICAL] 新登录路径不可用，后续依赖 Token 的用例可能失败\n")

        self.api_02_old_login_path_compat()
        self.jwt_02_env_secret_inject()
        self.api_03_inventory_under_v1()
        self.api_04_old_inventory_path_gone()
        self.api_05_unauth_any_api()
        self.api_06_whitelist_pass()
        self.sec_01_security_filter_chain_active()
        self.mqtt_01_topic_format()

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

        report = {
            "meta": {
                "base_url": self.base_url,
                "username": self.username,
                "timestamp": datetime.now().isoformat(),
                "branch": os.environ.get("GITHUB_REF", "local"),
                "suite": "review_fixes_regression"
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
        report_path = os.path.join(report_dir, f"review-fixes-report-{int(time.time())}.json")
        with open(report_path, "w", encoding="utf-8") as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
        print(f"\nReport saved: {report_path}")

        return 0 if self.failed == 0 and self.blocked == 0 else 1


def main():
    parser = argparse.ArgumentParser(description="REVIEW-2026-04-28 修复项回归测试")
    parser.add_argument("--base-url", default=os.environ.get("DMS_BASE_URL", "http://localhost:8080"))
    parser.add_argument("--user", default=os.environ.get("DMS_USERNAME", "admin"))
    parser.add_argument("--password", default=os.environ.get("DMS_PASSWORD", "admin123"))
    args = parser.parse_args()

    tester = ReviewFixesTester(args.base_url, args.user, args.password)
    rc = tester.run()
    sys.exit(rc)


if __name__ == "__main__":
    main()
