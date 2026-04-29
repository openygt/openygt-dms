import { onMounted, onUnmounted } from 'vue'

export interface ShortcutMap {
  [key: string]: (e: KeyboardEvent) => void | boolean
}

/**
 * 键盘快捷键组合式函数
 * @param shortcuts 快捷键映射表
 * @param options 配置项
 * @example
 * useKeyboardShortcuts({
 *   '/': () => searchRef.value?.focus(),
 *   'n': () => openDialog(),
 *   'r': () => fetchList(),
 *   'Escape': () => closeDialog(),
 *   'ctrl+1': () => router.push('/dashboard'),
 * })
 */
export function useKeyboardShortcuts(
  shortcuts: ShortcutMap,
  options: { target?: HTMLElement | Window; preventDefault?: boolean } = {}
) {
  const target = options.target || window

  function handler(e: Event) {
    const ke = e as KeyboardEvent
    const keys: string[] = []

    if (ke.ctrlKey || ke.metaKey) keys.push('ctrl')
    if (ke.altKey) keys.push('alt')
    if (ke.shiftKey) keys.push('shift')

    // 忽略在输入框、文本域中的快捷键（除非显式指定）
    const tag = (ke.target as HTMLElement)?.tagName
    const isInput = tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT'
    if (isInput && !keys.includes('ctrl') && !keys.includes('meta')) {
      // 单字符快捷键在输入框中不触发
      return
    }

    let key = ke.key
    if (key === ' ') key = 'Space'
    else if (key.length === 1) key = key.toLowerCase()

    keys.push(key)
    const combo = keys.join('+')

    const fn = shortcuts[combo] || shortcuts[key.toLowerCase()]
    if (fn) {
      const result = fn(ke)
      if (options.preventDefault !== false && result !== false) {
        ke.preventDefault()
      }
    }
  }

  onMounted(() => {
    target.addEventListener('keydown', handler)
  })

  onUnmounted(() => {
    target.removeEventListener('keydown', handler)
  })
}

/**
 * 带作用域的快捷键（仅在指定元素聚焦时触发）
 */
export function useScopedShortcuts(
  elRef: { value: HTMLElement | null },
  shortcuts: ShortcutMap
) {
  useKeyboardShortcuts(shortcuts, { target: elRef.value || undefined })
}
