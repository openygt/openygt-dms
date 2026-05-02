import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import Layout from '@/components/Layout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue') },
    {
      path: '/',
      component: Layout,
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '生产看板' } },
        { path: 'tasks', name: 'Tasks', component: () => import('@/views/task/TaskListView.vue'), meta: { title: '煎煮任务', perm: 'prod:task:list' } },
        { path: 'prescriptions', name: 'Prescriptions', component: () => import('@/views/prod/PrescriptionListView.vue'), meta: { title: '处方管理', perm: 'prod:prescription:list' } },
        { path: 'consume-log', name: 'ConsumeLog', component: () => import('@/views/inventory/ConsumeLogView.vue'), meta: { title: '药材消耗', perm: 'inv:log:list' } },
        { path: 'devices', name: 'Devices', component: () => import('@/views/eq/DeviceListView.vue'), meta: { title: '设备台账', perm: 'eq:device:list' } },
        { path: 'device-monitor', name: 'DeviceMonitor', component: () => import('@/views/eq/DeviceMonitorView.vue'), meta: { title: '设备监控', perm: 'eq:device:monitor' } },
        { path: 'device/:code/detail', name: 'DeviceDetail', component: () => import('@/views/eq/DeviceDetailView.vue'), meta: { title: '设备详情' } },
        { path: 'alarms', name: 'Alarms', component: () => import('@/views/monitor/AlarmLogView.vue'), meta: { title: '告警日志', perm: 'eq:alarm:list' } },
        { path: 'print-center', name: 'PrintCenter', component: () => import('@/views/prt/PrintCenterView.vue'), meta: { title: '打印中心', perm: 'prt:queue:view' } },
        { path: 'quality', name: 'Quality', component: () => import('@/views/qt/QualityListView.vue'), meta: { title: '质量检验', perm: 'qt:inspect:list' } },
        { path: 'capacity', name: 'Capacity', component: () => import('@/views/ops/CapacityReportView.vue'), meta: { title: '产能统计', perm: 'ops:capacity:view' } },
        { path: 'prod/receive', name: 'PrescriptionReceive', component: () => import('@/views/prod/PrescriptionReceiveView.vue'), meta: { title: '处方接收' } },
        { path: 'prod/delivery', name: 'DeliveryManage', component: () => import('@/views/prod/DeliveryManageView.vue'), meta: { title: '发药确认' } },
        { path: 'task-assignment', name: 'TaskAssignment', component: () => import('@/views/prod/TaskAssignmentView.vue'), meta: { title: '排产调度' } },
        { path: 'task-rollback', name: 'TaskRollback', component: () => import('@/views/prod/TaskRollbackView.vue'), meta: { title: '返工处理' } },
        { path: 'patient-query', name: 'PatientQuery', component: () => import('@/views/patient/PatientQueryView.vue'), meta: { title: '进度查询' } },
        { path: 'shelf-manage', name: 'ShelfManage', component: () => import('@/views/warehouse/ShelfManageView.vue'), meta: { title: '成品暂存' } },
        { path: 'voice-setting', name: 'VoiceSetting', component: () => import('@/views/pda/VoiceSettingView.vue'), meta: { title: '语音播报' } },
        { path: 'employee-barcode', name: 'EmployeeBarcode', component: () => import('@/views/system/EmployeeBarcodeView.vue'), meta: { title: '身份条码' } },
        { path: 'formula/package-spec', name: 'PackageSpec', component: () => import('@/views/formula/PackageSpecView.vue'), meta: { title: '包装规格' } },
        { path: 'trace/batch', name: 'BatchTrace', component: () => import('@/views/trace/BatchTraceView.vue'), meta: { title: '批次追溯' } },
        { path: 'trace/exception', name: 'ExceptionTrace', component: () => import('@/views/trace/ExceptionTraceView.vue'), meta: { title: '异常追溯' } },
        { path: 'report/qc-rate', name: 'QcRate', component: () => import('@/views/report/QcRateView.vue'), meta: { title: '合格统计' } },
        { path: 'print/template', name: 'LabelTemplate', component: () => import('@/views/print/LabelTemplateView.vue'), meta: { title: '标签打印' } },
        { path: 'print/printer', name: 'PrinterManage', component: () => import('@/views/print/PrinterManageView.vue'), meta: { title: '打印管理' } },
        { path: 'print/log', name: 'PrintLog', component: () => import('@/views/print/PrintLogView.vue'), meta: { title: '打印记录' } },
        { path: 'work-order-print', name: 'WorkOrderPrint', component: () => import('@/views/print/WorkOrderPrintView.vue'), meta: { title: '工单打印', perm: 'prt:workorder:view' } },
        { path: 'base/department', name: 'DepartmentManage', component: () => import('@/views/base/DepartmentManageView.vue'), meta: { title: '科室管理' } },
        { path: 'base/doctor', name: 'DoctorManage', component: () => import('@/views/base/DoctorManageView.vue'), meta: { title: '医师管理' } },
        { path: 'base/medicine', name: 'MedicineCatalog', component: () => import('@/views/base/MedicineCatalogView.vue'), meta: { title: '药材管理' } },
        { path: 'users', name: 'Users', component: () => import('@/views/system/UserListView.vue'), meta: { title: '人员管理', perm: 'sys:user:list' } },
        { path: 'sys/users', name: 'SysUsers', component: () => import('@/views/system/UserListView.vue'), meta: { title: '用户管理', perm: 'sys:user:list' } },
        { path: 'roles', name: 'Roles', component: () => import('@/views/system/RoleListView.vue'), meta: { title: '权限管理', perm: 'sys:role:list' } },
        { path: 'menus', name: 'Menus', component: () => import('@/views/system/MenuManageView.vue'), meta: { title: '菜单管理', perm: 'sys:menu:list' } },
        { path: 'configs', name: 'Configs', component: () => import('@/views/system/ConfigListView.vue'), meta: { title: '参数配置', perm: 'sys:config:list' } },
        { path: 'logs', name: 'Logs', component: () => import('@/views/system/SysLogView.vue'), meta: { title: '操作日志', perm: 'sys:log:list' } },
        { path: 'sys/logs', name: 'SysLogs', component: () => import('@/views/system/SysLogView.vue'), meta: { title: '系统日志', perm: 'sys:log:list' } },
        { path: 'interface-center', name: 'InterfaceCenter', component: () => import('@/views/system/InterfaceCenterView.vue'), meta: { title: '接口中心' } },
        { path: 'hospitals', name: 'Hospitals', component: () => import('@/views/md/HospitalListView.vue'), meta: { title: '医院管理', perm: 'md:hospital:list' } },
        { path: 'schemes', name: 'Schemes', component: () => import('@/views/md/SchemeListView.vue'), meta: { title: '煎药方案', perm: 'md:scheme:list' } },
        { path: 'traces', name: 'Traces', component: () => import('@/views/eq/DeviceTraceView.vue'), meta: { title: '处方追溯' } },
        { path: 'traces/:prescriptionNo', name: 'TraceDetail', component: () => import('@/views/eq/DeviceTraceDetailView.vue'), meta: { title: '追溯详情' } },
        { path: 'eq-dashboard', name: 'EqDashboard', component: () => import('@/views/eq/DashboardView.vue'), meta: { title: '数据看板' } },
        { path: 'workload', name: 'Workload', component: () => import('@/views/eq/WorkloadStatView.vue'), meta: { title: '工作量统计' } },
        { path: 'water-formulas', name: 'WaterFormulas', component: () => import('@/views/eq/WaterFormulaView.vue'), meta: { title: '加水公式' } },
        { path: 'prescription-defaults', name: 'PrescriptionDefaults', component: () => import('@/views/eq/PrescriptionDefaultView.vue'), meta: { title: '处方默认设置' } },
        { path: 'device-utilization', name: 'DeviceUtilization', component: () => import('@/views/eq/DeviceUtilizationView.vue'), meta: { title: '设备效能' } },
        { path: 'alarm-configs', name: 'AlarmConfigs', component: () => import('@/views/monitor/AlarmConfigView.vue'), meta: { title: '告警配置' } },
        { path: 'device-command', name: 'DeviceCommand', component: () => import('@/views/eq/DeviceCommandView.vue'), meta: { title: '设备操控', perm: 'eq:device:control' } },
        { path: 'device-maintenance', name: 'DeviceMaintenance', component: () => import('@/views/eq/DeviceMaintenanceView.vue'), meta: { title: '设备维保', perm: 'eq:maint:view' } },
        { path: 'step-visualization', name: 'StepVisualization', component: () => import('@/views/prod/StepVisualizationView.vue'), meta: { title: '流程跟踪' } },
        { path: 'herb-group', name: 'HerbGroup', component: () => import('@/views/prod/HerbGroupView.vue'), meta: { title: '分组投料' } },
        { path: 'time-monitor', name: 'TimeMonitor', component: () => import('@/views/monitor/TimeMonitorView.vue'), meta: { title: '时效监控' } },
        { path: 'emergency', name: 'EmergencyPrescription', component: () => import('@/views/prod/EmergencyPrescriptionView.vue'), meta: { title: '急诊快速通道' } },
        { path: 'temperature-curve', name: 'TemperatureCurve', component: () => import('@/views/eq/TemperatureCurveView.vue'), meta: { title: '温曲查询', perm: 'eq:device:monitor' } },
        { path: 'digital-twin', name: 'DigitalTwin', component: () => import('@/views/digital-twin/DigitalTwinView.vue'), meta: { title: '数字孪生', perm: 'eq:device:monitor' } },
        { path: 'toxic-medicine', name: 'ToxicMedicine', component: () => import('@/views/toxic-medicine/ToxicMedicineView.vue'), meta: { title: '毒性药材管理', perm: 'base:toxic:manage' } },
        { path: 'exception-order', name: 'ExceptionOrder', component: () => import('@/views/exception-order/ExceptionOrderView.vue'), meta: { title: '异常工单', perm: 'prod:exception:manage' } },
        { path: 'retain-sample', name: 'RetainSample', component: () => import('@/views/retain-sample/RetainSampleView.vue'), meta: { title: '留样管理', perm: 'qt:retain:manage' } },
        { path: 'wash-record', name: 'WashRecord', component: () => import('@/views/wash-record/WashRecordView.vue'), meta: { title: '清洗记录', perm: 'eq:wash:view' } },
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  if (to.path === '/login') {
    next()
    return
  }
  if (!userStore.isLoggedIn) {
    next('/login')
    return
  }
  // 开发环境跳过权限检查（数字孪生演示用）
  // if (to.meta.perm && !userStore.hasPermission(to.meta.perm as string)) {
  //   next('/dashboard')
  //   return
  // }
  next()
})

export default router
