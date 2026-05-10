import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface DeviceState {
  deviceCode: string
  name: string
  deviceType?: number
  status: string
  detailStatus: string
  currentTemp: number
  targetTemp: number
  remainingTime: number
  progressPercent: number
  currentPrescriptionCode?: string
  currentOperatorName?: string
  estimatedFinishTime?: string
  waterLevel?: number
  pressure?: number
  faultCode?: string
  lastHeartbeat?: string
  manufacturer?: string
  modelNum?: string
  packageNum?: number
  packageCapacity?: number
  printCopies?: number
  printStatus?: string
  labelMode?: string
  groupId?: number
  groupName?: string
  _networkOffline?: boolean
}

export const useDeviceStore = defineStore('device', () => {
  const devices = ref<Map<string, DeviceState>>(new Map())
  const alarms = ref<any[]>([])
  const connected = ref(false)
  const connecting = ref(false)

  const deviceList = computed(() => Array.from(devices.value.values()))

  const onlineCount = computed(() =>
    deviceList.value.filter(d => d.status !== 'OFFLINE').length
  )

  const faultCount = computed(() =>
    deviceList.value.filter(d => d.detailStatus === 'FAULT' || d.status === 'FAULT').length
  )

  const offlineCount = computed(() =>
    deviceList.value.filter(d => d.status === 'OFFLINE').length
  )

  function updateDevice(deviceCode: string, data: Partial<DeviceState>) {
    const existing = devices.value.get(deviceCode)
    if (existing) {
      devices.value.set(deviceCode, { ...existing, ...data })
    } else {
      devices.value.set(deviceCode, {
        deviceCode,
        name: data.name || deviceCode,
        status: data.status || 'IDLE',
        detailStatus: data.detailStatus || 'IDLE',
        currentTemp: data.currentTemp || 0,
        targetTemp: data.targetTemp || 0,
        remainingTime: data.remainingTime || 0,
        progressPercent: data.progressPercent || 0,
        ...data
      })
    }
    // ★ 创建新的 Map 实例以触发 Vue 响应式更新
    devices.value = new Map(devices.value)
  }

  function updateDevices(snapshot: DeviceState[]) {
    snapshot.forEach(d => updateDevice(d.deviceCode, d))
  }

  function removeDevice(deviceCode: string) {
    devices.value.delete(deviceCode)
  }

  function addAlarm(alarm: any) {
    alarms.value.unshift(alarm)
    if (alarms.value.length > 50) {
      alarms.value = alarms.value.slice(0, 50)
    }
  }

  function clearAlarms() {
    alarms.value = []
  }

  function setConnected(val: boolean) {
    connected.value = val
  }

  function setConnecting(val: boolean) {
    connecting.value = val
  }

  return {
    devices,
    deviceList,
    alarms,
    connected,
    connecting,
    onlineCount,
    faultCount,
    offlineCount,
    updateDevice,
    updateDevices,
    removeDevice,
    addAlarm,
    clearAlarms,
    setConnected,
    setConnecting
  }
})
