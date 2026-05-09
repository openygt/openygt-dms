-- ============================================================
-- 表名前缀统一：收敛旧 t_ 前缀，按业务域归类
-- 变更日期: 2026-05-09
-- ============================================================

-- 1. 医院基础数据域 (eq_)
RENAME TABLE `t_department` TO `eq_department`;
RENAME TABLE `t_doctor` TO `eq_doctor`;
RENAME TABLE `t_medicine` TO `eq_medicine`;

-- 2. 主数据域 (md_)
RENAME TABLE `t_package_spec` TO `md_package_spec`;

-- 3. 打印域 (prt_)
RENAME TABLE `t_label_template` TO `prt_label_template`;

-- 4. 生产业务域 (prod_)
RENAME TABLE `t_delivery_record` TO `prod_delivery_record`;

-- 5. 系统域 (sys_)
RENAME TABLE `t_interface_config` TO `sys_interface_config`;
RENAME TABLE `t_interface_log` TO `sys_interface_log`;
