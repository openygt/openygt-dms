#!/usr/bin/env python3
"""
API 路径一致性静态扫描
========================
验证 REVIEW-2026-04-28 中 N2/N3 修复：
  1. 禁止出现旧路径 /api/inventory/consume 硬编码
  2. 所有 Controller 的 @RequestMapping 应尽量使用 XxxModule.API_PREFIX 常量
  3. API 路径应统一包含 /v1 版本号
  4. 拦截器 addPathPatterns/excludePathPatterns 白名单完整

用法:
    python api_path_consistency_test.py [project_root]

退出码:
    0  无 HIGH 级别告警
    1  存在 HIGH 级别告警
"""

import os
import sys
import re
import json
from pathlib import Path
from datetime import datetime


class ApiPathScanner:
    HIGH = "HIGH"
    MEDIUM = "MEDIUM"
    LOW = "LOW"

    def __init__(self, root: str):
        self.root = Path(root)
        self.issues: list[dict] = []
        self.controller_count = 0
        self.hardcoded_count = 0
        self.constant_count = 0

    def scan(self):
        java_files = list(self.root.rglob("src/main/java/**/*.java"))
        for f in java_files:
            self._scan_file(f)
        self._scan_interceptors()

    def _scan_file(self, f: Path):
        content = f.read_text(encoding="utf-8", errors="ignore")
        relative = str(f.relative_to(self.root))

        # Controller 检测
        if "@RestController" in content or "@Controller" in content:
            self.controller_count += 1
            self._check_controller_mapping(content, relative)

        # 旧路径硬编码检测
        if "/api/inventory/consume" in content:
            self.issues.append({
                "level": self.HIGH,
                "file": relative,
                "msg": "发现旧路径硬编码 `/api/inventory/consume`，应使用 InventoryModule.API_PREFIX (/api/v1/inv)"
            })

        # 非 v1 版本号检测（简单启发式）
        if "@RequestMapping(" in content or "@GetMapping(" in content or "@PostMapping(" in content:
            self._check_version_prefix(content, relative)

    def _check_controller_mapping(self, content: str, relative: str):
        # 查找 @RequestMapping("/api/v1/...") 硬编码
        hardcoded = re.findall(r'@RequestMapping\("(/api/v1/[^"]+)"\)', content)
        for path in hardcoded:
            self.hardcoded_count += 1
            self.issues.append({
                "level": self.MEDIUM,
                "file": relative,
                "msg": f"Controller 使用硬编码路径 `{path}`，建议改为 Module.API_PREFIX 常量引用"
            })

        # 查找使用常量的（正向鼓励）
        if "API_PREFIX" in content:
            self.constant_count += 1

    def _check_version_prefix(self, content: str, relative: str):
        # 检测 /api/ 后不是 v1 的路径（排除 Module.API_PREFIX 变量形式）
        patterns = re.findall(r'@(?:Get|Post|Put|Delete|Request)Mapping\("(/api/[^"]+)"\)', content)
        for path in patterns:
            if not path.startswith("/api/v1/") and not path.startswith("/api/inventory"):
                # 忽略错误路径和公开路径
                if path.startswith("/api/") and "auth" not in path:
                    self.issues.append({
                        "level": self.MEDIUM,
                        "file": relative,
                        "msg": f"API 路径 `{path}` 缺少 /v1 版本号前缀"
                    })

    def _scan_interceptors(self):
        # 扫描 WebMvcConfig 或拦截器注册
        config_files = list(self.root.rglob("**/WebMvcConfig.java"))
        for f in config_files:
            content = f.read_text(encoding="utf-8", errors="ignore")
            relative = str(f.relative_to(self.root))
            if "addPathPatterns(\"/api/**\")" in content or "addPathPatterns(\"/api/**\")" in content:
                self.issues.append({
                    "level": self.LOW,
                    "file": relative,
                    "msg": "AuthInterceptor 已覆盖 /api/**，符合 L2 修复要求"
                })
            else:
                # 如果仍然只拦截 /api/v1/**
                if 'addPathPatterns("/api/v1/**")' in content or "addPathPatterns(\"/api/v1/**\")" in content:
                    self.issues.append({
                        "level": self.HIGH,
                        "file": relative,
                        "msg": "AuthInterceptor 仍只拦截 /api/v1/**，未覆盖 /api/**，存在绕过风险 (L2)"
                    })

            # 检查白名单是否包含新的 rbac/auth
            if "/api/v1/rbac/auth/" not in content:
                self.issues.append({
                    "level": self.HIGH,
                    "file": relative,
                    "msg": "拦截器白名单未排除 /api/v1/rbac/auth/**，登录接口会被拦截 (D5)"
                })

    def report(self):
        high = [i for i in self.issues if i["level"] == self.HIGH]
        medium = [i for i in self.issues if i["level"] == self.MEDIUM]
        low = [i for i in self.issues if i["level"] == self.LOW]

        print(f"\n{'='*60}")
        print("API 路径一致性扫描报告")
        print(f"{'='*60}")
        print(f"扫描 Controller 数量: {self.controller_count}")
        print(f"硬编码路径数量: {self.hardcoded_count}")
        print(f"使用 API_PREFIX 常量: {self.constant_count}")
        print(f"HIGH   : {len(high)}")
        print(f"MEDIUM : {len(medium)}")
        print(f"LOW    : {len(low)}")
        print(f"{'='*60}\n")

        for level, items in [(self.HIGH, high), (self.MEDIUM, medium), (self.LOW, low)]:
            for item in items:
                icon = {"HIGH": "🔴", "MEDIUM": "🟡", "LOW": "🟢"}.get(level, "⚪")
                print(f"{icon} [{level}] {item['file']}")
                print(f"    → {item['msg']}")

        # 保存 JSON
        report_dir = self.root / "tests" / "reports"
        report_dir.mkdir(parents=True, exist_ok=True)
        report_path = report_dir / f"api-path-scan-{int(datetime.now().timestamp())}.json"
        with open(report_path, "w", encoding="utf-8") as f:
            json.dump({
                "meta": {"timestamp": datetime.now().isoformat(), "root": str(self.root)},
                "summary": {"high": len(high), "medium": len(medium), "low": len(low),
                            "controllers": self.controller_count},
                "details": self.issues
            }, f, ensure_ascii=False, indent=2)
        print(f"\nReport saved: {report_path}")

        return 1 if high else 0


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else "."
    scanner = ApiPathScanner(root)
    scanner.scan()
    rc = scanner.report()
    sys.exit(rc)


if __name__ == "__main__":
    main()
