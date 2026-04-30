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
        { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '实时看板' } },
        { path: 'tasks', name: 'Tasks', component: () => import('@/views/task/TaskListView.vue'), meta: { title: '任务管理', perm: 'prod:task:list' } },
        { path: 'prescriptions', name: 'Prescriptions', component: () => import('@/views/prod/PrescriptionListView.vue'), meta: { title: '处方管理', perm: 'prod:prescription:list' } },
        { path: 'consume-log', name: 'ConsumeLog', component: () => import('@/views/inventory/ConsumeLogView.vue'), meta: { title: '药材消耗', perm: 'inv:log:list' } },
        { path: 'devices', name: 'Devices', component: () => import('@/views/eq/DeviceListView.vue'), meta: { title: '设备管理', perm: 'eq:device:list' } },
        { path: 'device-monitor', name: 'DeviceMonitor', component: () => import('@/views/eq/DeviceMonitorView.vue'), meta: { title: '设备监控', perm: 'eq:device:monitor' } },
        { path: 'device/:code/detail', name: 'DeviceDetail', component: () => import('@/views/eq/DeviceDetailView.vue'), meta: { title: '设备详情' } },
        { path: 'alarms', name: 'Alarms', component: () => import('@/views/monitor/AlarmLogView.vue'), meta: { title: '告警日志', perm: 'eq:alarm:list' } },
        { path: 'print-center', name: 'PrintCenter', component: () => import('@/views/prt/PrintCenterView.vue'), meta: { title: '打印管理', perm: 'prt:queue:view' } },
        { path: 'quality', name: 'Quality', component: () => import('@/views/qt/QualityListView.vue'), meta: { title: '质检管理', perm: 'qt:inspect:list' } },
        { path: 'capacity', name: 'Capacity', component: () => import('@/views/ops/CapacityReportView.vue'), meta: { title: '产能报表', perm: 'ops:capacity:view' } },
        { path: 'prod/receive', name: 'PrescriptionReceive', component: () => import('@/views/prod/PrescriptionReceiveView.vue'), meta: { title: '处方接收' } },
        { path: 'prod/delivery', name: 'DeliveryManage', component: () => import('@/views/prod/DeliveryManageView.vue'), meta: { title: '交付管理' } },
        { path: 'formula/package-spec', name: 'PackageSpec', component: () => import('@/views/formula/PackageSpecView.vue'), meta: { title: '包装规格' } },
        { path: 'trace/batch', name: 'BatchTrace', component: () => import('@/views/trace/BatchTraceView.vue'), meta: { title: '批次追溯' } },
        { path: 'trace/exception', name: 'ExceptionTrace', component: () => import('@/views/trace/ExceptionTraceView.vue'), meta: { title: '异常追溯' } },
        { path: 'report/qc-rate', name: 'QcRate', component: () => import('@/views/report/QcRateView.vue'), meta: { title: '质检合格率' } },
        { path: 'print/template', name: 'LabelTemplate', component: () => import('@/views/print/LabelTemplateView.vue'), meta: { title: '标签模板' } },
        { path: 'print/printer', name: 'PrinterManage', component: () => import('@/views/print/PrinterManageView.vue'), meta: { title: '打印机管理' } },
        { path: 'print/log', name: 'PrintLog', component: () => import('@/views/print/PrintLogView.vue'), meta: { title: '打印记录' } },
        { path: 'base/department', name: 'DepartmentManage', component: () => import('@/views/base/DepartmentManageView.vue'), meta: { title: '科室管理' } },
        { path: 'base/doctor', name: 'DoctorManage', component: () => import('@/views/base/DoctorManageView.vue'), meta: { title: '医师管理' } },
        { path: 'base/medicine', name: 'MedicineCatalog', component: () => import('@/views/base/MedicineCatalogView.vue'), meta: { title: '药材目录' } },
        { path: 'users', name: 'Users', component: () => import('@/views/system/UserListView.vue'), meta: { title: '用户管理', perm: 'sys:user:list' } },
        { path: 'roles', name: 'Roles', component: () => import('@/views/system/RoleListView.vue'), meta: { title: '角色管理', perm: 'sys:role:list' } },
        { path: 'menus', name: 'Menus', component: () => import('@/views/system/MenuManageView.vue'), meta: { title: '菜单管理', perm: 'sys:menu:list' } },
        { path: 'configs', name: 'Configs', component: () => import('@/views/system/ConfigListView.vue'), meta: { title: '系统配置', perm: 'sys:config:list' } },
        { path: 'logs', name: 'Logs', component: () => import('@/views/system/SysLogView.vue'), meta: { title: '系统日志', perm: 'sys:log:list' } },
        { path: 'hospitals', name: 'Hospitals', component: () => import('@/views/md/HospitalListView.vue'), meta: { title: '医院管理', perm: 'md:hospital:list' } },
        { path: 'schemes', name: 'Schemes', component: () => import('@/views/md/SchemeListView.vue'), meta: { title: '煎煮方案', perm: 'md:scheme:list' } },
        { path: 'traces', name: 'Traces', component: () => import('@/views/eq/DeviceTraceView.vue'), meta: { title: '煎药追溯' } },
        { path: 'traces/:prescriptionNo', name: 'TraceDetail', component: () => import('@/views/eq/DeviceTraceDetailView.vue'), meta: { title: '追溯详情' } },
        { path: 'eq-dashboard', name: 'EqDashboard', component: () => import('@/views/eq/DashboardView.vue'), meta: { title: '数据看板' } },
        { path: 'workload', name: 'Workload', component: () => import('@/views/eq/WorkloadStatView.vue'), meta: { title: '工作量统计' } },
        { path: 'water-formulas', name: 'WaterFormulas', component: () => import('@/views/eq/WaterFormulaView.vue'), meta: { title: '加水量公式' } },
        { path: 'prescription-defaults', name: 'PrescriptionDefaults', component: () => import('@/views/eq/PrescriptionDefaultView.vue'), meta: { title: '处方默认设置' } },
        { path: 'device-utilization', name: 'DeviceUtilization', component: () => import('@/views/eq/DeviceUtilizationView.vue'), meta: { title: '设备利用率' } },
        { path: 'alarm-configs', name: 'AlarmConfigs', component: () => import('@/views/monitor/AlarmConfigView.vue'), meta: { title: '告警配置' } },
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
  if (to.meta.perm && !userStore.hasPermission(to.meta.perm as string)) {
    next('/dashboard')
    return
  }
  next()
})

export default router
