#!/bin/bash
# 团队文档变更监控脚本 v2.0
# 职责：后台常驻，检测 docs/tasks/*.md 变更，生成待审阅清单
# 启动方式：nohup bash docs/tasks/.watchdog.sh > /dev/null 2>&1 &

WATCH_DIR="/data2/docker/decoction/docs/tasks"
LOG_FILE="$WATCH_DIR/.watchdog.log"
STATE_FILE="$WATCH_DIR/.watchdog.state"
PENDING_FILE="$WATCH_DIR/pending-review.md"
INTERVAL=30

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

log "监控启动，目录: $WATCH_DIR"

# 初始化状态文件
> "$STATE_FILE"
for f in "$WATCH_DIR"/*.md; do
    [ -f "$f" ] && stat -c "%n %Y" "$f" >> "$STATE_FILE" 2>/dev/null
done

# 初始化待审阅文件
if [ ! -f "$PENDING_FILE" ]; then
    cat > "$PENDING_FILE" << 'EOF'
# 📬 待审阅更新汇总

> 本文件由 watchdog 自动生成，架构师审阅后请清空「未读变更」表格

## 未读变更
| 时间 | 成员 | 文件 | 变更类型 |
|------|------|------|----------|

## 审阅记录
| 时间 | 架构师操作 |
|------|-----------|
EOF
fi

while true; do
    sleep "$INTERVAL"
    NEW_STATE=$(mktemp)
    CHANGED=0
    for f in "$WATCH_DIR"/*.md; do
        [ -f "$f" ] || continue
        BASENAME=$(basename "$f")
        # 跳过系统文件
        [[ "$BASENAME" == .watchdog* ]] && continue
        [[ "$BASENAME" == pending-review.md ]] && continue
        MTIME=$(stat -c "%Y" "$f" 2>/dev/null)
        echo "$f $MTIME" >> "$NEW_STATE"
        OLD_MTIME=$(grep -F "$f " "$STATE_FILE" 2>/dev/null | awk '{print $2}')
        if [ -n "$OLD_MTIME" ] && [ "$OLD_MTIME" != "$MTIME" ]; then
            log "文件变更: $BASENAME"
            # 解析成员名
            MEMBER=$(echo "$BASENAME" | grep -oP '^kimi0[1-4]' || echo "unknown")
            # 追加到 pending-review.md（在未读变更表格中插入一行）
            TMP_PENDING=$(mktemp)
            awk -v t="$(date '+%Y-%m-%d %H:%M:%S')" -v m="$MEMBER" -v f="$BASENAME" '
            /^## 未读变更/ { print; getline; print; print "| " t " | " m " | " f " | 内容更新 |"; next }
            { print }
            ' "$PENDING_FILE" > "$TMP_PENDING" && mv "$TMP_PENDING" "$PENDING_FILE"
            CHANGED=1
        elif [ -z "$OLD_MTIME" ]; then
            log "新文件: $BASENAME"
            CHANGED=1
        fi
    done
    mv "$NEW_STATE" "$STATE_FILE"
    
    # 如有变更，在 pending-review 顶部更新时间戳提示
    if [ "$CHANGED" -eq 1 ]; then
        log "已生成待审阅记录 → $PENDING_FILE"
    fi
done
