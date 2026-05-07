import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

function findMenuByPath(menus: any[], path: string): any | null {
  for (const menu of menus) {
    if (menu.path === path) return menu
    if (menu.children) {
      const found = findMenuByPath(menu.children, path)
      if (found) return found
    }
  }
  return null
}

export function useMenuDesc() {
  const userStore = useUserStore()
  const route = useRoute()

  const title = computed(() => {
    const menu = findMenuByPath(userStore.menus, route.path)
    return menu?.name || menu?.title || route.meta?.title || ''
  })

  const description = computed(() => {
    const menu = findMenuByPath(userStore.menus, route.path)
    return menu?.description || ''
  })

  return { title, description }
}
