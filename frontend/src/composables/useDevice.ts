import { ref, onMounted, onUnmounted } from 'vue'

/**
 * 设备类型检测 Composable
 * 用于 H5 响应式适配，区分 PC 端和移动端
 */
export function useDevice() {
  const isMobile = ref(false)
  const isTablet = ref(false)

  const checkDevice = () => {
    const width = window.innerWidth
    const userAgent = navigator.userAgent.toLowerCase()
    const isMobileUA = /mobile|android|iphone|ipad|ipod|windows phone/.test(userAgent)
    
    isMobile.value = width <= 768 || isMobileUA
    isTablet.value = width > 768 && width <= 1024
  }

  onMounted(() => {
    checkDevice()
    window.addEventListener('resize', checkDevice)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', checkDevice)
  })

  return {
    isMobile,
    isTablet
  }
}

export default useDevice
