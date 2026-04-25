#!/bin/bash
# 团队进度检查脚本 - 由 kimi01 在每次交互时执行
# 用法: bash docs/tasks/check-progress.sh

WATCH_DIR="/data2/docker/decoction/docs/tasks"
LOG_FILE="$WATCH_DIR/.watchdog.log"

echo "=== 最新文件变更 ==="
if [ -f "$LOG_FILE" ]; then
    tail -10 "$LOG_FILE"
else
    echo "(无监控日志)"
fi

echo ""
echo "=== 各成员回复文件状态 ==="
for f in "$WATCH_DIR"/kimi*-reply.md; do
    [ -f "$f" ] || continue
    BASENAME=$(basename "$f")
    MTIME=$(stat -c "%y" "$f" 2>/dev/null)
    # 提取状态行
    STATUS=$(grep -m1 "当前状态" "$f" 2>/dev/null | sed 's/.*- \[x\] //; s/.*- \[ \] //')
    printf "%-30s %-20s %s\n" "$BASENAME" "[$STATUS]" "$MTIME"
done

echo ""
echo "=== 各成员任务文件 ==="
ls -lt "$WATCH_DIR"/kimi*-task.md "$WATCH_DIR"/kimi*-概设.md 2>/dev/null | awk '{print $6,$7,$8,$9}'

echo ""
echo "=== 编译状态 ==="
cd /data2/docker/decoction
if mvn compile -q > /dev/null 2>&1; then
    echo "mvn compile: ✅ 通过"
else
    echo "mvn compile: ❌ 失败"
fi

echo ""
echo "=== 测试状态 ==="
if [ -d target/surefire-reports ]; then
    PASS=$(grep -l "Failures: 0, Errors: 0" target/surefire-reports/*.txt 2>/dev/null | wc -l)
    TOTAL=$(ls target/surefire-reports/*.txt 2>/dev/null | wc -l)
    echo "测试: $PASS/$TOTAL 类全部通过"
else
    echo "测试: (未执行)"
fi
