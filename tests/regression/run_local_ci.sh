#!/usr/bin/env bash
# ================================================
# V2.0 Enhancement 本地回归门禁脚本
# 工程师F / test/v2-enhancement-regression
#
# 用法:
#   cd /data2/docker/decoction/openygt-dms
#   bash tests/regression/run_local_ci.sh [base_url] [username] [password]
#
# 默认参数:
#   base_url=http://localhost:8080
#   username=admin
#   password=admin123
#
# 退出码:
#   0  全量通过
#   1  有失败/阻塞（禁止合流到 integration）
# ================================================

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
USERNAME="${2:-admin}"
PASSWORD="${3:-admin123}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
REPORT_DIR="${PROJECT_ROOT}/tests/reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_FILE="${REPORT_DIR}/regression-${TIMESTAMP}.log"

echo "========================================"
echo "DMS V2.0 本地回归门禁"
echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "目标: ${BASE_URL}"
echo "========================================"

mkdir -p "${REPORT_DIR}"
{
  echo "REGRESSION START: ${TIMESTAMP}"
  echo "BASE_URL: ${BASE_URL}"
  echo "BRANCH: $(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
  echo "COMMIT: $(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')"
  echo ""
} > "${REPORT_FILE}"

# ---- 前置检查 ----
echo ""
echo "[1/4] 前置检查..."

# 检查服务是否可达
if ! curl -sf "${BASE_URL}/actuator/health" >/dev/null 2>&1; then
  echo "  ⚠️  服务健康检查端点未响应，尝试根路径..."
  if ! curl -sf "${BASE_URL}/" >/dev/null 2>&1; then
    echo "  ❌ 目标服务 ${BASE_URL} 不可达，回归中止"
    echo "STATUS: BLOCKED (服务不可达)" >> "${REPORT_FILE}"
    exit 1
  fi
fi
echo "  ✅ 服务可达"

# 检查 Python + requests
if ! python3 -c "import requests" 2>/dev/null; then
  echo "  ⚠️  缺少 requests 库，尝试安装..."
  pip3 install requests -q || pip install requests -q
fi
echo "  ✅ Python requests 就绪"

# ---- API 冒烟测试 ----
echo ""
echo "[2/4] 执行 API 冒烟测试..."
export DMS_BASE_URL="${BASE_URL}"
export DMS_USERNAME="${USERNAME}"
export DMS_PASSWORD="${PASSWORD}"

SMOKE_RC=0
python3 "${SCRIPT_DIR}/api_smoke_test.py" 2>&1 | tee -a "${REPORT_FILE}" || SMOKE_RC=$?

if [ "${SMOKE_RC}" -ne 0 ]; then
  echo ""
  echo "❌ API 冒烟测试存在失败"
fi

# ---- 后端单元测试 ----
echo ""
echo "[3/4] 执行后端单元测试 (Maven)..."
MVN_RC=0
cd "${PROJECT_ROOT}"
if [ -f "pom.xml" ]; then
  # 使用 -pl 排除前端，仅测试 Java 模块
  mvn test -pl dms-common,dms-system,dms-rbac,dms-equipment,dms-inventory,dms-production,dms-quality,dms-print,dms-analytics,dms-masterdata -am -q 2>&1 | tee -a "${REPORT_FILE}" || MVN_RC=$?
  if [ "${MVN_RC}" -eq 0 ]; then
    echo "  ✅ Maven 测试通过"
  else
    echo "  ❌ Maven 测试存在失败"
  fi
else
  echo "  ⛔ 未找到 pom.xml，跳过 Maven 测试"
  MVN_RC=0
fi

# ---- 前端单元测试 ----
echo ""
echo "[4/4] 执行前端单元测试 (Vitest)..."
FE_RC=0
if [ -f "${PROJECT_ROOT}/frontend/package.json" ]; then
  cd "${PROJECT_ROOT}/frontend"
  if [ -d "node_modules" ]; then
    npm run test 2>&1 | tee -a "${REPORT_FILE}" || FE_RC=$?
    if [ "${FE_RC}" -eq 0 ]; then
      echo "  ✅ 前端单元测试通过"
    else
      echo "  ❌ 前端单元测试存在失败"
    fi
  else
    echo "  ⛔ 前端 node_modules 缺失，跳过"
    FE_RC=0
  fi
else
  echo "  ⛔ 未找到前端目录，跳过"
  FE_RC=0
fi

# ---- 汇总 ----
echo ""
echo "========================================"
echo "回归汇总"
echo "========================================"
TOTAL_RC=$((SMOKE_RC + MVN_RC + FE_RC))

if [ "${TOTAL_RC}" -eq 0 ]; then
  echo "✅ 全部通过，允许合流到 integration"
  echo "STATUS: PASS" >> "${REPORT_FILE}"
  exit 0
else
  echo "❌ 存在失败，禁止合流到 integration"
  echo "  冒烟测试 RC: ${SMOKE_RC}"
  echo "  Maven 测试 RC: ${MVN_RC}"
  echo "  前端测试 RC: ${FE_RC}"
  echo "STATUS: FAIL (smoke=${SMOKE_RC}, mvn=${MVN_RC}, fe=${FE_RC})" >> "${REPORT_FILE}"
  exit 1
fi
