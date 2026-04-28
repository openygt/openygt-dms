#!/usr/bin/env bash
# REVIEW-2026-04-28 修复项 回归测试总入口
# ========================================
# 用法:
#   cd /data2/docker/decoction/openygt-dms-01/openygt-dms
#   bash tests/regression/run_all_regression.sh [base_url]
#
# 执行顺序:
#   1. 静态扫描（不依赖服务启动）
#   2. 白盒配置扫描（不依赖服务启动）
#   3. 启动应用（使用 test-env.yml）
#   4. API 回归测试
#   5. JWT 安全专项
#   6. 启动日志扫描
#   7. 生成汇总报告

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
BASE_URL="${1:-http://localhost:8080}"
REPORT_DIR="${PROJECT_ROOT}/tests/reports"
mkdir -p "${REPORT_DIR}"

SUMMARY_JSON="${REPORT_DIR}/regression-summary-$(date +%s).json"

echo "========================================"
echo "REVIEW-2026-04-28 修复项 回归测试总入口"
echo "Project Root: ${PROJECT_ROOT}"
echo "Base URL: ${BASE_URL}"
echo "========================================"

# ---------- 1. 静态扫描 ----------
echo -e "\n[1/5] API 路径一致性静态扫描..."
cd "${PROJECT_ROOT}"
python3 tests/regression/api_path_consistency_test.py "${PROJECT_ROOT}" || true

# ---------- 2. MQTT Topic 白盒扫描 ----------
echo -e "\n[2/5] MQTT Topic 白盒扫描..."
python3 tests/regression/mqtt_topic_test.py "${PROJECT_ROOT}" || true

# ---------- 3. 编译与单元测试 ----------
echo -e "\n[3/5] Maven 编译与单元测试..."
cd "${PROJECT_ROOT}"
mvn clean test -q || true

# ---------- 4. 启动应用（测试配置）----------
echo -e "\n[4/5] 启动应用（测试配置）..."
# 备份原配置
if [ -f "${PROJECT_ROOT}/dms-app/src/main/resources/application.yml" ]; then
    cp "${PROJECT_ROOT}/dms-app/src/main/resources/application.yml" \
       "${PROJECT_ROOT}/dms-app/src/main/resources/application.yml.bak"
fi
cp "${PROJECT_ROOT}/tests/regression/test-env.yml" \
   "${PROJECT_ROOT}/dms-app/src/main/resources/application.yml"

# 后台启动
mvn spring-boot:run -pl dms-app -q &
APP_PID=$!
echo "App PID: ${APP_PID}"

# 等待启动（最多 60 秒）
for i in $(seq 1 60); do
    if curl -sf "${BASE_URL}/actuator/health" >/dev/null 2>&1 || curl -sf "${BASE_URL}/error" >/dev/null 2>&1; then
        echo "应用已就绪 (${i}s)"
        break
    fi
    sleep 1
done

# ---------- 5. API 回归测试 ----------
echo -e "\n[5/5] API 回归测试..."
python3 tests/regression/review_fixes_regression.py --base-url "${BASE_URL}" || true

# ---------- 6. JWT 安全专项 ----------
echo -e "\n[6/5] JWT 安全专项测试..."
python3 tests/regression/jwt_security_test.py --base-url "${BASE_URL}" || true

# ---------- 7. 启动日志扫描 ----------
echo -e "\n[7/5] 启动日志扫描..."
python3 tests/regression/startup_log_scan.py || true

# ---------- 清理 ----------
echo -e "\n[Cleanup] 停止应用..."
kill ${APP_PID} 2>/dev/null || true
wait ${APP_PID} 2>/dev/null || true

# 恢复配置
if [ -f "${PROJECT_ROOT}/dms-app/src/main/resources/application.yml.bak" ]; then
    mv "${PROJECT_ROOT}/dms-app/src/main/resources/application.yml.bak" \
       "${PROJECT_ROOT}/dms-app/src/main/resources/application.yml"
fi

# ---------- 汇总报告 ----------
echo -e "\n========================================"
echo "汇总报告"
echo "========================================"

REPORTS=("${REPORT_DIR}"/*-report-*.json)
python3 - <<'PYEOF'
import json, glob, sys, os
from datetime import datetime

report_dir = os.environ.get("REPORT_DIR", "tests/reports")
files = sorted(glob.glob(f"{report_dir}/*-report-*.json"))

suites = []
overall_pass = 0
overall_fail = 0
overall_block = 0

for f in files:
    try:
        with open(f) as fh:
            data = json.load(fh)
        meta = data.get("meta", {})
        summary = data.get("summary", {})
        suite = meta.get("suite", os.path.basename(f))
        total = summary.get("total", 0)
        passed = summary.get("passed", summary.get("pass", 0))
        failed = summary.get("failed", summary.get("fail", 0))
        blocked = summary.get("blocked", 0)
        suites.append({
            "suite": suite,
            "file": os.path.basename(f),
            "total": total,
            "passed": passed,
            "failed": failed,
            "blocked": blocked
        })
        overall_pass += passed
        overall_fail += failed
        overall_block += blocked
    except Exception as e:
        print(f"解析失败 {f}: {e}")

summary_data = {
    "meta": {"timestamp": datetime.now().isoformat()},
    "overall": {
        "total_passed": overall_pass,
        "total_failed": overall_fail,
        "total_blocked": overall_block,
        "result": "PASS" if overall_fail == 0 and overall_block == 0 else "FAIL"
    },
    "suites": suites
}

summary_path = os.path.join(report_dir, f"regression-summary-{int(datetime.now().timestamp())}.json")
with open(summary_path, "w") as f:
    json.dump(summary_data, f, ensure_ascii=False, indent=2)

print(f"\n总用例: {overall_pass + overall_fail + overall_block}")
print(f"通过  : {overall_pass}")
print(f"失败  : {overall_fail}")
print(f"阻塞  : {overall_block}")
print(f"结果  : {summary_data['overall']['result']}")
print(f"\n汇总报告: {summary_path}")
PYEOF

echo -e "\n========================================"
echo "回归测试执行完毕"
echo "========================================"
