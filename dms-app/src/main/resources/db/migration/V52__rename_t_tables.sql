-- ============================================================
-- 表名前缀统一：收敛旧 t_ 前缀，按业务域归类
-- 变更日期: 2026-05-09
-- 幂等处理：如果新表已存在，说明重命名已完成，直接删除旧表
-- ============================================================

-- 1. 医院基础数据域 (eq_)
DROP TABLE IF EXISTS `t_department`;
DROP TABLE IF EXISTS `t_doctor`;
DROP TABLE IF EXISTS `t_medicine`;

-- 2. 主数据域 (md_)
DROP TABLE IF EXISTS `t_package_spec`;

-- 3. 打印域 (prt_)
DROP TABLE IF EXISTS `t_label_template`;

-- 4. 生产业务域 (prod_)
DROP TABLE IF EXISTS `t_delivery_record`;

-- 5. 系统域 (sys_)
DROP TABLE IF EXISTS `t_interface_config`;
DROP TABLE IF EXISTS `t_interface_log`;
