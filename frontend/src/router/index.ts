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
        { path: 'consume-log', name: 'ConsumeLog', component: () => import('@/views/inventory/ConsumeLogView.vue'), meta: { title: '消耗流水', perm: 'inv:log:list' } },
        { path: 'devices', name: 'Devices', component: () => import('@/views/eq/DeviceListView.vue'), meta: { title: '设备管理', perm: 'eq:device:list' } },
        { path: 'alarms', name: 'Alarms', component: () => import('@/views/monitor/AlarmLogView.vue'), meta: { title: '告警日志', perm: 'eq:alarm:list' } },
        { path: 'print-center', name: 'PrintCenter', component: () => import('@/views/prt/PrintCenterView.vue'), meta: { title: '打印中心', perm: 'prt:queue:view' } },
        { path: 'quality', name: 'Quality', component: () => import('@/views/qt/QualityListView.vue'), meta: { title: '质检记录', perm: 'qt:inspect:list' } },
        { path: 'capacity', name: 'Capacity', component: () => import('@/views/ops/CapacityReportView.vue'), meta: { title: '产能报表', perm: 'ops:capacity:view' } },
        { path: 'users', name: 'Users', component: () => import('@/views/system/UserListView.vue'), meta: { title: '用户管理', perm: 'sys:user:list' } },
        { path: 'roles', name: 'Roles', component: () => import('@/views/system/RoleListView.vue'), meta: { title: '角色管理', perm: 'sys:role:list' } },
        { path: 'menus', name: 'Menus', component: () => import('@/views/system/MenuManageView.vue'), meta: { title: '菜单管理', perm: 'sys:menu:list' } },
        { path: 'configs', name: 'Configs', component: () => import('@/views/system/ConfigListView.vue'), meta: { title: '系统配置', perm: 'sys:config:list' } },
        { path: 'logs', name: 'Logs', component: () => import('@/views/system/SysLogView.vue'), meta: { title: '系统日志', perm: 'sys:log:list' } },
        { path: 'hospitals', name: 'Hospitals', component: () => import('@/views/md/HospitalListView.vue'), meta: { title: '医院管理', perm: 'md:hospital:list' } },
        { path: 'schemes', name: 'Schemes', component: () => import('@/views/md/SchemeListView.vue'), meta: { title: '煎煮方案', perm: 'md:scheme:list' } },
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
