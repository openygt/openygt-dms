ALTER TABLE sys_menu ADD COLUMN description VARCHAR(200)
  COMMENT '页面功能描述，用于标题区副标题展示' AFTER title;

-- 初始化数据（61条）
-- 处方管理
UPDATE sys_menu SET description = '新增、导入、查询处方，处理异常' WHERE path = '/prescriptions';
UPDATE sys_menu SET description = '急诊处方优先处理、保障时效' WHERE path = '/emergency';
UPDATE sys_menu SET description = '审核处方、确认接收、退回异常' WHERE path = '/prod/receive';

-- 生产指挥
UPDATE sys_menu SET description = '查看今日任务、设备状态、告警' WHERE path = '/dashboard';
UPDATE sys_menu SET description = '登记和查看每日工作记录' WHERE path = '/task-assignment';
UPDATE sys_menu SET description = '选择任务查看工序进度' WHERE path = '/step-visualization';
UPDATE sys_menu SET description = '按日期、设备、员工统计产量' WHERE path = '/capacity';
UPDATE sys_menu SET description = '查询用户操作和系统事件记录' WHERE path = '/sys/logs';
UPDATE sys_menu SET description = '语音播报和分组投料规则入口' WHERE path = '/production-setting';

-- 设备管理
UPDATE sys_menu SET description = '管理设备信息，配对生产线' WHERE path = '/devices';
UPDATE sys_menu SET description = '配置设备通信参数' WHERE path = '/device-network';
UPDATE sys_menu SET description = '设备分组和生产线配对规则' WHERE path = '/device-group-manage';
UPDATE sys_menu SET description = '查看设备运行状态和告警' WHERE path = '/device-monitor';
UPDATE sys_menu SET description = '远程控制设备启停和急停' WHERE path = '/device-command';
UPDATE sys_menu SET description = '查看和处理设备告警' WHERE path = '/alarms';
UPDATE sys_menu SET description = '维护计划、保养记录和提醒' WHERE path = '/device-maintenance';
UPDATE sys_menu SET description = '查看设备清洗记录' WHERE path LIKE '%wash%';

-- 煎药作业
UPDATE sys_menu SET description = '看板式管理煎药任务' WHERE path = '/tasks';
UPDATE sys_menu SET description = '药材分组管理和扫码确认投料' WHERE path = '/herb-group';
UPDATE sys_menu SET description = '配置生产现场语音提醒参数' WHERE path = '/voice-setting';

-- 质量检验
UPDATE sys_menu SET description = '质检记录和合格判定' WHERE path = '/quality';
UPDATE sys_menu SET description = '留样记录管理和到期预警' WHERE path LIKE '%retain%';
UPDATE sys_menu SET description = '处理和升级生产异常' WHERE path LIKE '%exception%';
UPDATE sys_menu SET description = '发起和处理返工申请' WHERE path = '/task-rollback';
UPDATE sys_menu SET description = '查看煎煮过程温度曲线' WHERE path = '/temperature-curve';
UPDATE sys_menu SET description = '质检合格率统计' WHERE path = '/report/qc-rate';

-- 发药管理
UPDATE sys_menu SET description = '成品入库出库和库存管理' WHERE path = '/shelf-manage';
UPDATE sys_menu SET description = '按处方发药和签收' WHERE path = '/prod/delivery';
UPDATE sys_menu SET description = '扫码或手机号查询煎药进度' WHERE path = '/patient-query';

-- 追溯查询
UPDATE sys_menu SET description = '处方全链路追溯' WHERE path = '/traces';
UPDATE sys_menu SET description = '按批次号追溯生产过程' WHERE path = '/trace/batch';
UPDATE sys_menu SET description = '异常处理过程追溯' WHERE path = '/trace/exception';

-- 工艺配置
UPDATE sys_menu SET description = '配置煎药工艺参数' WHERE path = '/schemes';
UPDATE sys_menu SET description = '设置不同方剂的加水量' WHERE path = '/water-formulas';
UPDATE sys_menu SET description = '配置药包规格和标签' WHERE path = '/formula/package-spec';
UPDATE sys_menu SET description = '设置设备告警规则' WHERE path = '/alarm-configs';

-- 基础数据
UPDATE sys_menu SET description = '维护医院信息和科室关系' WHERE path = '/hospitals';
UPDATE sys_menu SET description = '维护科室信息' WHERE path LIKE '%department%';
UPDATE sys_menu SET description = '维护医师信息和处方权限' WHERE path LIKE '%doctor%';
UPDATE sys_menu SET description = '维护药材目录' WHERE path LIKE '%medicine%';
UPDATE sys_menu SET description = '管理成品存放货架' WHERE path LIKE '%finished-shelf%';
UPDATE sys_menu SET description = '管理毒性药材' WHERE path LIKE '%toxic%';
UPDATE sys_menu SET description = '维护员工信息、分配角色' WHERE path = '/users';
UPDATE sys_menu SET description = '生成和打印员工条码' WHERE path LIKE '%barcode%';

-- 打印中心
UPDATE sys_menu SET description = '打印药包标签' WHERE path = '/print/template';
UPDATE sys_menu SET description = '配置打印机和查看状态' WHERE path = '/print/printer';
UPDATE sys_menu SET description = '查询打印历史' WHERE path = '/print/log';
UPDATE sys_menu SET description = '打印煎药工单' WHERE path LIKE '%work-order%';

-- 系统管理
UPDATE sys_menu SET description = '维护员工信息和分配角色' WHERE path = '/sys/users';
UPDATE sys_menu SET description = '分配角色和菜单权限' WHERE path = '/roles';
UPDATE sys_menu SET description = '调整菜单结构和顺序' WHERE path = '/menus';
UPDATE sys_menu SET description = '设置系统运行参数' WHERE path = '/configs';

-- 独立页面
UPDATE sys_menu SET description = '设备运行数据和趋势' WHERE path LIKE '%eq-dashboard%';
UPDATE sys_menu SET description = '按人员设备统计工作量' WHERE path LIKE '%workload%';
UPDATE sys_menu SET description = '查看设备利用率和效率' WHERE path LIKE '%device-util%';
UPDATE sys_menu SET description = '设置处方默认参数' WHERE path LIKE '%prescription-default%';
UPDATE sys_menu SET description = '监控各工序倒计时和超时' WHERE path = '/time-monitor';
UPDATE sys_menu SET description = '查看药材领用和消耗记录' WHERE path = '/consume-log';
UPDATE sys_menu SET description = '第三方系统对接配置' WHERE path = '/interface-center';
