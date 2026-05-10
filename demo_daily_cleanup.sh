#!/bin/bash
# ============================================
# Demo环境每日数据清理脚本（保守版）
# 原则：只清理运行日志和临时数据，绝不碰核心业务表和基础配置
# ============================================

set -uo pipefail

DB_HOST="127.0.0.1"
DB_PORT="3306"
DB_USER="root"
DB_PASS="mysql_pwd01"
DB_NAME="openygt_dms_clean"
LOG_FILE="/var/log/openygt-dms-demo-cleanup.log"
DATE=$(date '+%Y-%m-%d %H:%M:%S')

mkdir -p "$(dirname "$LOG_FILE")"

# --------------------------------------------------
# 1. 检查最近24小时是否有试用痕迹
# --------------------------------------------------
USAGE_COUNT=$(mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS -N -e \
  "SELECT COUNT(*) FROM $DB_NAME.sys_log WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR);" 2>/dev/null || echo "0")

if [ "${USAGE_COUNT:-0}" -eq 0 ]; then
    echo "[$DATE] 无人试用，跳过清理" >> "$LOG_FILE"
    exit 0
fi

echo "[$DATE] 检测到试用痕迹（sys_log 24h内=$USAGE_COUNT），开始清理..." >> "$LOG_FILE"

# --------------------------------------------------
# 2. 清理运行日志（安全范围，每个语句独立执行防止互相影响）
# --------------------------------------------------

run_sql() {
    local sql="$1"
    local desc="$2"
    local result
    result=$(mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS -e "SET FOREIGN_KEY_CHECKS=0; $sql; SELECT ROW_COUNT();" 2>&1 || true)
    local exit_code=$?
    if [ $exit_code -eq 0 ]; then
        local rows=$(echo "$result" | tail -1 | tr -d ' ')
        echo "[$DATE] $desc 完成，影响行数: ${rows:-0}" >> "$LOG_FILE"
    else
        echo "[$DATE] $desc 失败: $result" >> "$LOG_FILE"
    fi
}

run_sql "DELETE FROM $DB_NAME.sys_log WHERE created_at < DATE_SUB(NOW(), INTERVAL 24 HOUR)" "清理sys_log"
run_sql "DELETE FROM $DB_NAME.sys_log_archive WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)" "清理sys_log_archive"
run_sql "DELETE FROM $DB_NAME.eq_temperature_log WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)" "清理eq_temperature_log"
run_sql "DELETE FROM $DB_NAME.eq_temperature_log_archive WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)" "清理eq_temperature_log_archive"
run_sql "DELETE FROM $DB_NAME.eq_device_alarm WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)" "清理eq_device_alarm"
run_sql "DELETE FROM $DB_NAME.eq_device_status WHERE snapshot_time < DATE_SUB(NOW(), INTERVAL 7 DAY)" "清理eq_device_status"
run_sql "DELETE FROM $DB_NAME.trc_prescription_trace WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)" "清理trc_prescription_trace"
run_sql "DELETE FROM $DB_NAME.trc_trace_event WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)" "清理trc_trace_event"
run_sql "DELETE FROM $DB_NAME.prod_step_log WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)" "清理prod_step_log"
run_sql "DELETE FROM $DB_NAME.prod_task_status_history WHERE operate_time < DATE_SUB(NOW(), INTERVAL 7 DAY)" "清理prod_task_status_history"
run_sql "DELETE FROM $DB_NAME.prod_alert_log WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)" "清理prod_alert_log"
run_sql "DELETE FROM $DB_NAME.prod_time_monitor WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)" "清理prod_time_monitor"

# --------------------------------------------------
# 3. 验证基础数据安全
# --------------------------------------------------
SYSUSER_COUNT=$(mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS -N -e "SELECT COUNT(*) FROM $DB_NAME.sys_user;" 2>/dev/null || echo "0")
DEVICE_COUNT=$(mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS -N -e "SELECT COUNT(*) FROM $DB_NAME.eq_device;" 2>/dev/null || echo "0")
TASK_COUNT=$(mysql -h$DB_HOST -P$DB_PORT -u$DB_USER -p$DB_PASS -N -e "SELECT COUNT(*) FROM $DB_NAME.prod_task;" 2>/dev/null || echo "0")

echo "[$DATE] 验证：sys_user=$SYSUSER_COUNT, eq_device=$DEVICE_COUNT, prod_task=$TASK_COUNT" >> "$LOG_FILE"

if [ "$SYSUSER_COUNT" -lt 1 ] || [ "$DEVICE_COUNT" -lt 1 ]; then
    echo "[$DATE] ERROR: 基础数据异常！sys_user=$SYSUSER_COUNT, eq_device=$DEVICE_COUNT" >> "$LOG_FILE"
    exit 1
fi

echo "[$DATE] 清理完成 ✅" >> "$LOG_FILE"
