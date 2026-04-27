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
    const res: any = await request.post('/auth/login', { username, password })
    token.value = res.data.token
    localStorage.setItem('token', token.value)
    await fetchUserInfo()
    await fetchMenus()
  }

  async function fetchUserInfo() {
    const res: any = await request.get('/system/user/info')
    userInfo.value = res.data
    permissions.value = res.data.permissions || []
  }

  async function fetchMenus() {
    const res: any = await request.get('/rbac/menus/tree')
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
