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

  async function login(username: string, password: string) {
    const res: any = await request.post('/v1/auth/login', { username, password })
    token.value = res.data.token
    localStorage.setItem('token', token.value)
    await fetchUserInfo()
    await fetchMenus()
  }

  async function fetchUserInfo() {
    // TokenResponse 已包含用户信息，无需额外接口
    try {
      const payload = token.value.split('.')[1]
      const tokenData = payload ? JSON.parse(atob(payload)) : {}
      userInfo.value = {
        id: tokenData.userId,
        username: tokenData.sub || tokenData.username,
        roles: tokenData.roles || []
      }
      permissions.value = tokenData.permissions || []
    } catch (e) {
      userInfo.value = { username: '未知用户', roles: [] }
      permissions.value = []
    }
  }

  async function fetchMenus() {
    const res: any = await request.get('/v1/rbac/menus/tree')
    menus.value = res.data || []
  }

  function hasPermission(perm: string) {
    return permissions.value.includes(perm)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    menus.value = []
    permissions.value = []
    localStorage.removeItem('token')
    router.push('/login')
  }

  return { token, userInfo, menus, permissions, isLoggedIn, login, logout, fetchUserInfo, fetchMenus, hasPermission }
})
