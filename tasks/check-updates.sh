#!/bin/bash
# 团队沟通文件巡检脚本
# 用法: bash docs/tasks/check-updates.sh

echo "=== 团队沟通文件巡检 $(date '+%Y-%m-%d %H:%M:%S') ==="
echo ""

for member in kimi01 kimi02 kimi03 kimi04; do
    file="docs/tasks/${member}-reply.md"
    if [ -f "$file" ]; then
        mtime=$(stat -c '%Y' "$file")
        mtime_human=$(date -d "@$mtime" '+%Y-%m-%d %H:%M:%S')
        size=$(stat -c '%s' "$file")
        echo "[$member] 最后更新: $mtime_human | 大小: ${size}B"
        
        # 检查是否在过去5分钟内有更新
        now=$(date +%s)
        diff=$((now - mtime))
        if [ $diff -lt 300 ]; then
            echo "  ⚠️  最近5分钟内有更新！"
        fi
    else
        echo "[$member] 文件不存在: $file"
    fi
done

echo ""
echo "=== 巡检完成 ==="
