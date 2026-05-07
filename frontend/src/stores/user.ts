import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/api/request'
import { syncRequestAuthorization } from '@/api/request'
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
    syncRequestAuthorization(token.value)
    fetchUserInfo()
  }

  async function login(username: string, password: string) {
    const res: any = await request.post('/v1/rbac/auth/login', { username, password })
    token.value = res.data.token
    localStorage.setItem('token', res.data.token)
    syncRequestAuthorization(res.data.token)
    await fetchUserInfo()
    // TODO: 动态菜单方案待产品确认，当前侧栏为静态编码
    // await fetchMenus()
  }

  function fetchUserInfo() {
    // TokenResponse 已包含用户信息，无需额外接口（同步执行，确保刷新后立即恢复权限）
    try {
      const payload = token.value.split('.')[1]
      const tokenData = payload ? JSON.parse(atob(payload)) : {}
      const subId = tokenData.sub != null && tokenData.sub !== '' ? Number(tokenData.sub) : undefined
      userInfo.value = {
        id: tokenData.userId ?? (Number.isFinite(subId) ? subId : undefined),
        username: tokenData.username || '未知用户',
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
    const perms = permissions.value
    // 管理员/主任拥有全部权限
    if (perms.includes('ROLE_ADMIN') || perms.includes('ROLE_DIRECTOR')) {
      return true
    }

    // 各角色权限集合
    const workerPerms = [
      'prod:task:view', 'prod:trace:view', 'prod:dosing:view', 'prod:voice:view',
      'eq:device:monitor', 'eq:temp:view', 'qt:inspect:view',
      'trace:rx:view', 'trace:batch:view', 'trace:exc:view', 'sys:log:view',
      'sys:user:update', 'sys:user:change-password'
    ]
    const inspectorPerms = [
      'qt:inspect:view', 'qt:yield:view', 'eq:temp:view',
      'trace:rx:view', 'trace:batch:view', 'trace:exc:view', 'sys:log:view',
      'eq:device:monitor', 'prod:progress:view'
    ]
    const leaderPerms = [
      'ops:dashboard:view', 'prod:record:view', 'prod:monitor:view',
      'eq:alarm:view', 'eq:device:monitor', 'ops:capacity:view', 'eq:device:efficiency',
      'prod:task:view', 'prod:trace:view', 'prod:dosing:view', 'prod:voice:view',
      'qt:inspect:view', 'prod:rework:view', 'eq:temp:view', 'qt:yield:view',
      'inv:storage:view', 'prod:dispatch:view', 'prod:progress:view',
      'trace:rx:view', 'trace:batch:view', 'trace:exc:view', 'sys:log:view',
      'md:scheme:view', 'eq:water:view', 'md:package:view', 'eq:alarm:view',
      'md:hospital:view', 'md:dept:view', 'md:doctor:view', 'md:herb:view', 'md:shelf:view',
      'sys:user:view', 'sys:barcode:view',
      'prt:label:view', 'prt:printer:view', 'prt:log:view',
      'sys:user:create', 'sys:user:delete', 'sys:role:assign',
      'eq:device:create', 'eq:device:update', 'eq:device:delete'
    ]

    if (perms.includes('ROLE_WORKER') && workerPerms.includes(perm)) return true
    if (perms.includes('ROLE_INSPECTOR') && inspectorPerms.includes(perm)) return true
    if (perms.includes('ROLE_LEADER') && leaderPerms.includes(perm)) return true

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
    syncRequestAuthorization('')
    router.push('/login')
  }

  return { token, userInfo, menus, permissions, isLoggedIn, login, logout, fetchUserInfo, fetchMenus, hasPermission, displayRoles }
})
