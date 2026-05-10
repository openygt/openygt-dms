#!/bin/bash
# ============================================================
# OpenYGT-DMS AI实例启动脚本
# 必须指定端口，且只能是9091-9099
# 用法: ./start.sh <端口>
# 示例: ./start.sh 9091
# ============================================================

PORT=$1

# 检查是否传了端口
if [ -z "$PORT" ]; then
    echo "错误: 必须指定端口"
    echo "用法: ./start.sh <端口>"
    echo "可用端口: 9091-9099"
    exit 1
fi

# 检查端口是否在9091-9099范围内
if [ "$PORT" -lt 9091 ] || [ "$PORT" -gt 9099 ]; then
    echo "错误: 端口 ${PORT} 不在允许范围"
    echo "可用端口: 9091-9099"
    exit 1
fi

# 检查端口是否已被占用（grep用--分隔）
PID=$(ps aux | grep "dms-app" | grep -- "--server.port=${PORT}" | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    echo "错误: 端口 ${PORT} 已被占用，PID: ${PID}"
    echo "请先执行: ./stop.sh ${PORT}"
    exit 1
fi

# 最优JVM参数（固定）
JVM_OPTS="-Xms128m -Xmx256m -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 全相对路径启动
nohup java ${JVM_OPTS} -jar ./dms-app/target/dms-app-1.0.0.jar \
    --server.port=${PORT} \
    --server.address=0.0.0.0 \
    --spring.profiles.active=dev \
    --spring.datasource.hikari.maximum-pool-size=2 \
    --spring.datasource.hikari.minimum-idle=1 \
    --logging.level.root=WARN \
    > ./tomcat.log 2>&1 &

echo "tomcat已启动，端口: ${PORT}，PID: $!"
