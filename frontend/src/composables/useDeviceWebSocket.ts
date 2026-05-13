import { onMounted, onUnmounted } from 'vue'
import { Client, IMessage, StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useDeviceStore } from '@/stores/device'
import { ElMessage } from 'element-plus'

let client: Client | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let subscriptions: StompSubscription[] = []

const WS_ENDPOINT = '/ws/iot'
const RECONNECT_INTERVAL = 5000
const MAX_RECONNECT_ATTEMPTS = 10

let reconnectAttempts = 0

export function useDeviceWebSocket(tenantId: string = 'default') {
  const deviceStore = useDeviceStore()

  async function connect() {
    if (client?.active) return
    if (deviceStore.connecting) return

    deviceStore.setConnecting(true)

    // 能力探测
    try {
      const token = localStorage.getItem('token') || ''
      const res = await fetch('/api/v1/eq/ws/capability', {
        headers: { Authorization: `Bearer ${token}` }
      })
      const capability = await res.json()
      console.log('WebSocket capability:', capability.data)
    } catch (e) {
      console.warn('WebSocket capability check failed')
    }

    const token = localStorage.getItem('token') || ''

    client = new Client({
      webSocketFactory: () => new SockJS(WS_ENDPOINT),
      connectHeaders: {
        Authorization: 'Bearer ' + token
      },
      debug: (str) => {
        // console.log('STOMP: ' + str)
      },
      reconnectDelay: 0, // 我们手动控制重连
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        console.log('WebSocket connected')
        deviceStore.setConnected(true)
        deviceStore.setConnecting(false)
        reconnectAttempts = 0

        // 订阅设备状态
        subscribeToDeviceStatus()

        // 订阅租户设备快照
        subscribeToTenantSnapshot(tenantId)

        // 订阅告警
        subscribeToAlarms(tenantId)
      },
      onDisconnect: () => {
        console.log('WebSocket disconnected')
        deviceStore.setConnected(false)
        scheduleReconnect(tenantId)
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message'])
        deviceStore.setConnected(false)
        scheduleReconnect(tenantId)
      },
      onWebSocketError: (event) => {
        console.error('WebSocket error:', event)
        deviceStore.setConnected(false)
        scheduleReconnect(tenantId)
      }
    })

    client.activate()
  }

  function subscribeToDeviceStatus() {
    if (!client?.active) return

    // 通配符订阅（如果支持）
    const sub = client.subscribe('/topic/device/+/status', (message: IMessage) => {
      try {
        const payload = JSON.parse(message.body)
        deviceStore.updateDevice(payload.deviceCode, {
          status: payload.status,
          detailStatus: payload.detail?.detailStatus || payload.status,
          currentTemp: payload.detail?.currentTemp,
          remainingTime: payload.detail?.remainingTime,
          progressPercent: payload.detail?.progressPercent,
          currentPrescriptionCode: payload.detail?.prescriptionCode,
          currentOperatorName: payload.detail?.operatorName,
          waterLevel: payload.detail?.waterLevel,
          pressure: payload.detail?.pressure,
          lastHeartbeat: new Date().toISOString()
        })
      } catch (e) {
        console.error('Failed to parse device status:', e)
      }
    })
    subscriptions.push(sub)
  }

  function subscribeToTenantSnapshot(tenantId: string) {
    if (!client?.active) return

    const sub = client.subscribe(`/topic/tenant/${tenantId}/devices/snapshot`, (message: IMessage) => {
      try {
        const payload = JSON.parse(message.body)
        if (payload.devices) {
          deviceStore.updateDevices(payload.devices)
        }
      } catch (e) {
        console.error('Failed to parse snapshot:', e)
      }
    })
    subscriptions.push(sub)
  }

  function subscribeToAlarms(tenantId: string) {
    if (!client?.active) return

    const sub = client.subscribe(`/topic/tenant/${tenantId}/alarms`, (message: IMessage) => {
      try {
        const payload = JSON.parse(message.body)
        deviceStore.addAlarm(payload)

        // 显示告警通知
        if (payload.alarmType === 'TEMP_HIGH') {
          ElMessage.warning(`${payload.deviceCode} 温度超高: ${payload.message}`)
        } else if (payload.alarmType === 'FAULT') {
          ElMessage.error(`${payload.deviceCode} 故障: ${payload.message}`)
        } else if (payload.alarmType === 'OFFLINE') {
          ElMessage.warning(`${payload.deviceCode} 已离线`)
        }
      } catch (e) {
        console.error('Failed to parse alarm:', e)
      }
    })
    subscriptions.push(sub)
  }

  function scheduleReconnect(tenantId: string) {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
    }

    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
      console.error('Max reconnect attempts reached')
      deviceStore.setConnecting(false)
      return
    }

    reconnectAttempts++
    const delay = Math.min(RECONNECT_INTERVAL * reconnectAttempts, 30000)

    reconnectTimer = setTimeout(() => {
      console.log(`Reconnecting... attempt ${reconnectAttempts}`)
      connect()
    }, delay)
  }

  function disconnect() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    subscriptions.forEach(sub => sub.unsubscribe())
    subscriptions = []
    client?.deactivate()
    client = null
    deviceStore.setConnected(false)
  }

  function sendCommand(deviceCode: string, commandType: string, params?: any) {
    if (!client?.active) {
      ElMessage.warning('WebSocket未连接，指令无法发送')
      return false
    }

    client.publish({
      destination: '/app/device/command',
      body: JSON.stringify({
        deviceCode,
        commandType,
        params,
        timestamp: Date.now()
      })
    })
    return true
  }

  onMounted(() => {
    connect()
  })

  onUnmounted(() => {
    disconnect()
  })

  return {
    connect,
    disconnect,
    sendCommand
  }
}
