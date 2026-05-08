-- 留样业务统一为「7天留样」单条：废弃原 1/2/3 类型语义，历史数据归一并去重

-- 1. 全部有效记录统一为 7 天留样（类型 4、168 小时、到期=留样时间+7 天）
UPDATE qt_retain_sample
SET sample_type     = 4,
    retain_duration = 168,
    expire_time     = DATE_ADD(retain_time, INTERVAL 7 DAY),
    updated_at      = CURRENT_TIMESTAMP
WHERE deleted = 0;

-- 2. 每任务仅保留一条（保留 id 最大的一条，其余逻辑删除）
UPDATE qt_retain_sample r
    INNER JOIN (
    SELECT task_id, MAX(id) AS keep_id
    FROM qt_retain_sample
    WHERE deleted = 0
    GROUP BY task_id
) k ON r.task_id = k.task_id AND r.id <> k.keep_id
SET r.deleted    = 1,
    r.updated_at = CURRENT_TIMESTAMP;

-- 3. 字段注释与当前业务一致
ALTER TABLE qt_retain_sample
    MODIFY COLUMN sample_type TINYINT NOT NULL COMMENT '7天留样固定为4';
