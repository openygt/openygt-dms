import { ref, watch } from 'vue'

const STORAGE_KEY = 'ygt-theme'
type Theme = 'light' | 'dark' | 'auto'

const theme = ref<Theme>(
  (localStorage.getItem(STORAGE_KEY) as Theme) || 'auto'
)

function applyTheme(value: Theme) {
  const isDark = value === 'dark' ||
    (value === 'auto' && matchMedia('(prefers-color-scheme: dark)').matches)
  document.documentElement.classList.toggle('dark', isDark)
}

watch(theme, (v) => {
  localStorage.setItem(STORAGE_KEY, v)
  applyTheme(v)
}, { immediate: true })

// 监听系统主题变化
if (typeof window !== 'undefined') {
  const mq = matchMedia('(prefers-color-scheme: dark)')
  mq.addEventListener?.('change', () => {
    if (theme.value === 'auto') applyTheme('auto')
  })
}

export function useTheme() {
  return { theme, setTheme: (t: Theme) => { theme.value = t } }
}
