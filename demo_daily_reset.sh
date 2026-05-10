#!/bin/bash
# Demo环境每日重置：以当前状态为起点，truncate全库 → init.sql → 标记迁移已执行

DB="openygt_dms_clean"
HOST="127.0.0.1"
USER="root"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SQL="$SCRIPT_DIR/dms-app/src/main/resources/db/demo/init.sql"

# 1. TRUNCATE 全库所有表
TABLES=$(mysql -h$HOST -u$USER -N -e "
  SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
  WHERE TABLE_SCHEMA='$DB' AND TABLE_TYPE='BASE TABLE'
" 2>/dev/null)

{
    echo "SET FOREIGN_KEY_CHECKS=0;"
    for t in $TABLES; do
        echo "TRUNCATE TABLE \`$DB\`.\`$t\`;"
    done
    cat "$SQL"
    echo "SET FOREIGN_KEY_CHECKS=1;"
} | mysql -h$HOST -u$USER "$DB"

# 2. 标记所有迁移脚本已执行（防止服务启动时重新执行 V1 DROP TABLE）
mysql -h$HOST -u$USER "$DB" -e "
CREATE TABLE IF NOT EXISTS sys_migration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    script VARCHAR(100) NOT NULL UNIQUE,
    executed_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_migration (script) VALUES
('V1__baseline.sql'),
('V49__baseline_master_data.sql'),
('V50__baseline_industry_dict.sql'),
('V51__fix_hospital_import.sql'),
('V52__rename_t_tables.sql'),
('V53__rename_iot_tables.sql')
ON DUPLICATE KEY UPDATE executed_at = executed_at;
"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Demo重置完成"
