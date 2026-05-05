#!/bin/bash
# 必须指定端口，且只能是5171-5179
# 用法: ./start-frontend.sh <端口>

PORT=$1

if [ -z "$PORT" ]; then
    echo "错误: 必须指定端口"
    echo "用法: ./start-frontend.sh <端口>"
    echo "可用端口: 5171-5179"
    exit 1
fi

if [ "$PORT" -lt 5171 ] || [ "$PORT" -gt 5179 ]; then
    echo "错误: 端口 ${PORT} 不在允许范围"
    echo "可用端口: 5171-5179"
    exit 1
fi

nohup npm run dev -- --host 0.0.0.0 --port ${PORT} \
    > ./vite-${PORT}.log 2>&1 &

echo "前端已启动，端口: ${PORT}，PID: $!"
