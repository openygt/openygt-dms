#!/bin/bash
# Demo环境每日重置：truncate全库 → 执行init.sql

DB="openygt_dms_clean"
HOST="127.0.0.1"
USER="root"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SQL="$SCRIPT_DIR/dms-app/src/main/resources/db/demo/init.sql"

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
