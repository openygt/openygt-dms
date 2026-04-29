import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/api/request'
import router from '@/router'

export interface MenuItem {
  name: string
  path: string
  icon?: string
  children?: MenuItem[]
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<any>(null)
  const menus = ref<MenuItem[]>([])
  const permissions = ref<string[]>([])

  const isLoggedIn = computed(() => !!token.value)

  // 初始化：若本地有 token，自动恢复用户权限信息（解决刷新后权限丢失）
  if (token.value) {
    fetchUserInfo()
  }

  async function login(username: string, password: string) {
    const res: any = await request.post('/v1/rbac/auth/login', { username, password })
    token.value = res.data.token
    localStorage.setItem('token', res.data.token)
    await fetchUserInfo()
    await fetchMenus()
  }

  function fetchUserInfo() {
    // TokenResponse 已包含用户信息，无需额外接口（同步执行，确保刷新后立即恢复权限）
    try {
      const payload = token.value.split('.')[1]
      const tokenData = payload ? JSON.parse(atob(payload)) : {}
      userInfo.value = {
        id: tokenData.userId,
        username: tokenData.username || tokenData.sub || '未知用户',
        roles: tokenData.roles || []
      }
      permissions.value = tokenData.permissions || []
    } catch (e) {
      userInfo.value = { username: '未知用户', roles: [] }
      permissions.value = []
    }
  }

  async function fetchMenus() {
    try {
      const res: any = await request.get('/v1/rbac/menus/tree')
      menus.value = res.data || []
    } catch (e) {
      menus.value = []
    }
  }

  function hasPermission(perm: string) {
    // 当前系统 JWT permissions 为角色码，需做角色→权限映射
    const perms = permissions.value
    // 管理员/主任拥有全部权限
    if (perms.includes('ROLE_ADMIN') || perms.includes('ROLE_DIRECTOR')) {
      return true
    }
    // 班组长/操作工/质检员拥有基础操作权限
    const workerRoles = ['ROLE_LEADER', 'ROLE_WORKER', 'ROLE_INSPECTOR']
    const hasWorkerRole = perms.some((r: string) => workerRoles.includes(r))
    if (hasWorkerRole) {
      const workerPerms = [
        'prod:task:list', 'prod:prescription:list',
        'eq:device:list', 'eq:alarm:list',
        'prt:queue:view', 'qt:inspect:list',
        'inv:log:list', 'ops:capacity:view'
      ]
      if (workerPerms.includes(perm)) return true
    }
    return perms.includes(perm)
  }

  const roleNameMap: Record<string, string> = {
    'ROLE_ADMIN': '系统管理员',
    'ROLE_DIRECTOR': '主任',
    'ROLE_LEADER': '班组长',
    'ROLE_WORKER': '操作工',
    'ROLE_INSPECTOR': '质检员'
  }

  const displayRoles = computed(() => {
    const roles = userInfo.value?.roles || []
    return roles.map((r: string) => roleNameMap[r] || r)
  })

  function logout() {
    token.value = ''
    userInfo.value = null
    menus.value = []
    permissions.value = []
    localStorage.removeItem('token')
    router.push('/login')
  }

  return { token, userInfo, menus, permissions, isLoggedIn, login, logout, fetchUserInfo, fetchMenus, hasPermission, displayRoles }
})
