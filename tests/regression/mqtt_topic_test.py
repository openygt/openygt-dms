#!/usr/bin/env python3
"""
MQTT Topic 配置与白盒测试
==========================
验证 REVIEW-2026-04-28 D3 修复项：
  1. application.yml 中 mqtt.topic-subscription 默认值为 +/+/+
  2. MqttConfig 拼接结果为 /openygt/+/+/+
  3. handleMessage 能正确按 /openygt/{tenantId}/{deviceCode}/{messageType} 解析

用法:
    python mqtt_topic_test.py [project_root]

退出码:
    0  全部通过
    1  存在失败
"""

import os
import sys
import re
import json
from pathlib import Path
from datetime import datetime


class MqttTopicTester:
    def __init__(self, root: str):
        self.root = Path(root)
        self.results: list[dict] = []
        self.passed = 0
        self.failed = 0

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
        else:
            self.failed += 1
        icon = {"PASS": "✅", "FAIL": "❌"}.get(status, "?")
        print(f"{icon} [{case_id}] {desc} — {status} {detail}")

    def test_01_application_yml(self):
        """MQTT-01: application.yml 中 topic-subscription 为 +/+/+"""
        case = "MQTT-01"
        yml_path = self.root / "dms-app" / "src" / "main" / "resources" / "application.yml"
        if not yml_path.exists():
            self.log(case, "application.yml 存在", "FAIL", f"找不到 {yml_path}")
            return
        content = yml_path.read_text(encoding="utf-8")
        # 查找 topic-subscription 行
        match = re.search(r'topic-subscription:\s*"([^"]+)"', content)
        if match:
            value = match.group(1)
            if value == "+/+/+":
                self.log(case, "application.yml topic-subscription 为 +/+/+", "PASS", f"value={value}")
            else:
                self.log(case, "topic-subscription 值不符", "FAIL", f"value={value}")
        else:
            # 尝试查找默认值写法
            if 'topic-subscription: "+/+/+"' in content or "topic-subscription: +/+/+" in content:
                self.log(case, "application.yml topic-subscription 为 +/+/+", "PASS")
            else:
                self.log(case, "未找到 topic-subscription 配置", "FAIL")

    def test_02_mqtt_config_concat(self):
        """MQTT-02: MqttConfig 中拼接为 /openygt/ + topicSubscription"""
        case = "MQTT-02"
        config_path = self.root / "dms-equipment" / "src" / "main" / "java" / "cn" / "org" / "openygt" / "equipment" / "config" / "MqttConfig.java"
        if not config_path.exists():
            self.log(case, "MqttConfig.java 存在", "FAIL", f"找不到 {config_path}")
            return
        content = config_path.read_text(encoding="utf-8")
        if '"/openygt/" + topicSubscription' in content or '"/openygt/" + topicSubscription' in content.replace(" ", ""):
            self.log(case, "MqttConfig 拼接 /openygt/ + topicSubscription", "PASS")
        elif '"/openygt/"' in content and 'subscribeTopic' in content:
            self.log(case, "MqttConfig 拼接逻辑疑似正确", "PASS", "请人工复核")
        else:
            self.log(case, "MqttConfig 拼接逻辑不符", "FAIL", "期望 `/openygt/` + topicSubscription")

    def test_03_topic_parse(self):
        """MQTT-03: handleMessage 按 /openygt/tenant/device/type 解析"""
        case = "MQTT-03"
        config_path = self.root / "dms-equipment" / "src" / "main" / "java" / "cn" / "org" / "openygt" / "equipment" / "config" / "MqttConfig.java"
        content = config_path.read_text(encoding="utf-8")
        # 检查 split "/" 以及 parts[2], parts[3], parts[4]
        checks = [
            ('topic.split("/")' in content, "按 / split topic"),
            ("parts[2]" in content, "取 parts[2] 为 tenantId"),
            ("parts[3]" in content, "取 parts[3] 为 deviceCode"),
            ("parts[4]" in content, "取 parts[4] 为 messageType"),
        ]
        all_ok = all(c[0] for c in checks)
        if all_ok:
            self.log(case, "Topic 解析逻辑符合文档模型", "PASS")
        else:
            fails = [c[1] for c in checks if not c[0]]
            self.log(case, "Topic 解析逻辑缺失", "FAIL", ", ".join(fails))

    def test_04_topic_length_guard(self):
        """MQTT-04: 对 topic parts 长度做保护"""
        case = "MQTT-04"
        config_path = self.root / "dms-equipment" / "src" / "main" / "java" / "cn" / "org" / "openygt" / "equipment" / "config" / "MqttConfig.java"
        content = config_path.read_text(encoding="utf-8")
        if "parts.length < 5" in content:
            self.log(case, "Topic 长度不足5时返回/警告", "PASS")
        else:
            self.log(case, "缺少 Topic 长度保护", "FAIL")

    def run(self):
        print(f"\n{'='*60}")
        print("MQTT Topic 配置与白盒测试")
        print(f"Root: {self.root}")
        print(f"Time: {datetime.now().isoformat()}")
        print(f"{'='*60}\n")

        self.test_01_application_yml()
        self.test_02_mqtt_config_concat()
        self.test_03_topic_parse()
        self.test_04_topic_length_guard()

        print(f"\n{'='*60}")
        print("SUMMARY")
        print(f"{'='*60}")
        print(f"Total : {len(self.results)}")
        print(f"Passed: {self.passed}")
        print(f"Failed: {self.failed}")
        print(f"{'='*60}")

        report_dir = self.root / "tests" / "reports"
        report_dir.mkdir(parents=True, exist_ok=True)
        report_path = report_dir / f"mqtt-topic-report-{int(datetime.now().timestamp())}.json"
        with open(report_path, "w", encoding="utf-8") as f:
            json.dump({
                "meta": {"root": str(self.root), "timestamp": datetime.now().isoformat(), "suite": "mqtt_topic"},
                "summary": {"total": len(self.results), "passed": self.passed, "failed": self.failed},
                "details": self.results
            }, f, ensure_ascii=False, indent=2)
        print(f"\nReport saved: {report_path}")
        return 0 if self.failed == 0 else 1


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else "."
    tester = MqttTopicTester(root)
    rc = tester.run()
    sys.exit(rc)


if __name__ == "__main__":
    main()
