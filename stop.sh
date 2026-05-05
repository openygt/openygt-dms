#!/bin/bash
# ============================================================
# OpenYGT-DMS AI实例停止脚本
# 必须指定端口，且只能是9091-9099
# 用法: ./stop.sh <端口>
# 示例: ./stop.sh 9091
# ============================================================

PORT=$1

# 检查是否传了端口
if [ -z "$PORT" ]; then
    echo "错误: 必须指定端口"
    echo "用法: ./stop.sh <端口>"
    echo "可用端口: 9091-9099"
    exit 1
fi

# 检查端口是否在9091-9099范围内
if [ "$PORT" -lt 9091 ] || [ "$PORT" -gt 9099 ]; then
    echo "错误: 端口 ${PORT} 不在允许范围"
    echo "可用端口: 9091-9099"
    exit 1
fi

# 查找对应端口的进程（grep用--分隔，防止--server.port被当成选项）
PID=$(ps aux | grep "dms-app" | grep -- "--server.port=${PORT}" | grep -v grep | awk '{print $2}')

if [ -n "$PID" ]; then
    kill -9 $PID
    echo "tomcat已停止，端口: ${PORT}，PID: ${PID}"
else
    echo "端口 ${PORT} 的tomcat未运行"
fi
