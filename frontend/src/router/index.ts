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
        { path: 'consume-log', name: 'ConsumeLog', component: () => import('@/views/inventory/ConsumeLogView.vue'), meta: { title: '消耗流水', perm: 'inv:log:list' } },
        { path: 'alarms', name: 'Alarms', component: () => import('@/views/monitor/AlarmLogView.vue'), meta: { title: '告警日志', perm: 'eq:alarm:list' } },
        { path: 'users', name: 'Users', component: () => import('@/views/system/UserListView.vue'), meta: { title: '用户管理', perm: 'sys:user:list' } },
        { path: 'roles', name: 'Roles', component: () => import('@/views/system/RoleListView.vue'), meta: { title: '角色管理', perm: 'sys:role:list' } },
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
