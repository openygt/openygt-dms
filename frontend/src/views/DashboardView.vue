<template>
  <div class="dashboard">
    <div class="dashboard-page-head">
      <div>
      </div>
      <el-button type="primary" plain :loading="loading || alarmLoading" @click="refreshAll">刷新</el-button>
    </div>

    <el-alert
      v-if="dashboardError && !loading"
      type="error"
      :closable="false"
      show-icon
      class="dashboard-alert"
      :title="dashboardError"
    >
      <el-button size="small" type="primary" @click="fetchDashboard">重试看板数据</el-button>
    </el-alert>

    <!-- KPI 卡片矩阵 -->
    <div class="kpi-grid">
      <div v-for="(kpi, idx) in kpis" :key="idx" class="kpi-card" :class="`kpi-${kpi.type}`">
        <div class="kpi-top">
          <el-icon :size="20" class="kpi-icon"><component :is="kpi.icon" /></el-icon>
          <span class="kpi-label">{{ kpi.label }}</span>
        </div>
        <div class="kpi-value">
          <span class="ygt-num">{{ kpi.value }}</span>
          <span v-if="kpi.trend !== undefined && kpi.trend !== 0" class="kpi-trend" :class="{ up: kpi.trend > 0, down: kpi.trend < 0 }">
            <el-icon><CaretTop v-if="kpi.trend > 0" /><CaretBottom v-else-if="kpi.trend < 0" /></el-icon>
            {{ Math.abs(kpi.trend) }}%
          </span>
        </div>
        <div class="kpi-sub">
          {{ kpi.sub }}
          <span v-if="kpi.trend === 0" class="kpi-flat"> · 环比持平</span>
        </div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="quick-actions">
      <div
        v-for="action in quickActions"
        :key="action.path"
        class="quick-item"
        role="button"
        tabindex="0"
        @click="goQuick(action.path)"
        @keydown.enter.prevent="goQuick(action.path)"
        @keydown.space.prevent="goQuick(action.path)"
      >
        <div class="quick-icon" :style="{ background: action.bg }">
          <el-icon :size="20" color="#fff"><component :is="action.icon" /></el-icon>
        </div>
        <span class="quick-label">{{ action.label }}</span>
      </div>
    </div>

    <!-- 数据分布行 -->
    <div class="dashboard-row">
      <el-card class="dashboard-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>任务状态分布</span>
            <el-tag size="small" type="info">今日</el-tag>
          </div>
        </template>
        <LoadingState v-if="loading" description="加载任务数据..." />
        <div v-else-if="dashboardError" class="card-error">
          <p>看板任务数据未能加载</p>
          <el-button size="small" type="primary" @click="fetchDashboard">重试</el-button>
        </div>
        <EmptyState
          v-else-if="!taskDist.length"
          title="暂无任务数据"
          description="今日还没有生成任务"
        />
        <div v-else class="dist-list">
          <div v-for="(item, tIdx) in taskDist" :key="'ts-' + tIdx + '-' + (item.status || 'x')" class="dist-item">
            <div class="dist-label">
              <span class="dist-dot" :style="{ background: statusColor(item.status) }"></span>
              <span>{{ statusLabel(item.status) }}</span>
            </div>
            <div class="dist-bar-wrap">
              <div class="dist-bar" :style="{ width: distPercent(item.count, maxTaskCount) + '%', background: statusColor(item.status) }"></div>
            </div>
            <span class="dist-value ygt-num">{{ item.count }}</span>
          </div>
        </div>
      </el-card>

      <el-card class="dashboard-card" shadow="none">
        <template #header>
          <div class="card-header">
            <span>设备类型分布</span>
            <div>
              <el-tag size="small" type="info">总数 / 在线 / 在线异常</el-tag>
              <el-button size="small" type="primary" style="margin-left: 8px" @click="$router.push('/device-monitor')">进入监控</el-button>
            </div>
          </div>
        </template>
        <LoadingState v-if="loading" description="加载设备数据..." />
        <div v-else-if="dashboardError" class="card-error">
          <p>看板设备数据未能加载</p>
          <el-button size="small" type="primary" @click="fetchDashboard">重试</el-button>
        </div>
        <EmptyState
          v-else-if="!deviceDist.length"
          title="暂无设备数据"
          description="还没有录入设备信息"
        />
        <div v-else>
          <div class="device-legend" aria-label="设备条图例">
            <span><span class="legend-swatch legend-offline" aria-hidden="true" />离线</span>
            <span><span class="legend-swatch legend-online" aria-hidden="true" />在线</span>
            <span><span class="legend-swatch legend-alarm" aria-hidden="true" />在线且异常</span>
          </div>
          <div class="dist-list">
          <div v-for="(item, dIdx) in deviceDist" :key="'dt-' + dIdx + '-' + (item.deviceType || 'x')" class="dist-item">
            <div class="dist-label">
              <span class="dist-dot" :style="{ background: deviceColor(item.deviceType) }"></span>
              <span>{{ deviceTypeName(item.deviceType) }}</span>
            </div>
            <div class="dist-bar-wrap" :style="{ width: exactPercent(item.count, maxDeviceCount) + '%' }">
              <div class="dist-bar-stack">
                <div class="dist-bar-online" :style="{ flex: Math.max(0, item.flexOnlineNormal || 0), background: deviceColor(item.deviceType) }"></div>
                <div class="dist-bar-alarm" :style="{ flex: Math.max(0, item.flexAlarm || 0) }"></div>
                <div class="dist-bar-gray" :style="{ flex: Math.max(0, item.flexOffline || 0) }"></div>
              </div>
            </div>
            <span class="dist-value ygt-num">
              {{ item.count }}/{{ item.onlineCount }}/{{ item.onlineWithAlarmCount }}
            </span>
          </div>
        </div>
        </div>
      </el-card>
    </div>

    <!-- 异常预警列表 -->
    <el-card shadow="never" style="margin-top: var(--ygt-space-4)" data-testid="dashboard-alarm-card">
      <template #header>
        <div class="card-header">
          <span>异常预警</span>
          <div>
            <el-tag v-if="alarmLoading" size="small" type="info">加载中</el-tag>
            <el-tag v-else-if="alarmError" size="small" type="warning">告警未加载</el-tag>
            <el-tag v-else-if="alarmList.length > 0" size="small" type="danger">{{ alarmList.length }} 条待处理</el-tag>
            <el-tag v-else size="small" type="success">无异常</el-tag>
            <el-button size="small" type="primary" style="margin-left: 8px" @click="$router.push('/alarms')">查看全部</el-button>
          </div>
        </div>
      </template>
      <LoadingState v-if="alarmLoading" description="加载告警数据..." />
      <div v-else-if="alarmError" class="card-error">
        <p>{{ alarmError }}</p>
        <el-button size="small" type="primary" @click="fetchAlarms">重试</el-button>
      </div>
      <EmptyState
        v-else-if="alarmList.length === 0"
        title="暂无异常预警"
        description="系统运行正常，未检测到异常"
      />
      <div v-else class="alarm-list">
        <div
          v-for="(alarm, aIdx) in alarmList.slice(0, 5)"
          :key="alarm.id != null ? 'al-' + alarm.id : 'al-i-' + aIdx"
          class="alarm-item"
          :class="alarmItemClass(alarm)"
        >
          <div class="alarm-main">
            <div class="alarm-top">
              <el-tag size="small" :type="alarmLevelType(alarm.alarmLevel)">{{ alarm.alarmLevel || 'INFO' }}</el-tag>
              <span class="alarm-device">{{ alarm.deviceCode || '-' }}</span>
              <span class="alarm-time">{{ formatDateTime(alarm.createdAt) }}</span>
            </div>
            <el-tooltip :content="alarmDisplayText(alarm)" placement="top" :disabled="!alarmDisplayText(alarm) || alarmDisplayText(alarm).length < 32">
              <div class="alarm-content">{{ alarmDisplayText(alarm) }}</div>
            </el-tooltip>
          </div>
          <el-button
            size="small"
            type="success"
            :loading="resolvingAlarmId === alarm.id"
            :disabled="resolvingAlarmId !== null && resolvingAlarmId !== alarm.id"
            @click="resolveAlarm(alarm.id)"
          >
            处理
          </el-button>
        </div>
        <div v-if="alarmTotal > 5" class="alarm-more">
          <el-link type="primary" @click="$router.push('/alarms')">还有 {{ alarmTotal - 5 }} 条，点击查看全部</el-link>
        </div>
      </div>
    </el-card>

    <!-- 系统信息 -->
    <el-card shadow="never" style="margin-top: var(--ygt-space-4)">
      <template #header>
        <div class="card-header">
          <span>系统信息</span>
        </div>
      </template>
      <div class="sys-info">
        <div class="sys-row">
          <span class="sys-label">系统版本</span>
          <span class="sys-value">OpenYGT-DMS 智能煎药管理系统 v1.0</span>
        </div>
        <div class="sys-row">
          <span class="sys-label">当前用户</span>
          <span class="sys-value">{{ userStore.userInfo?.username || '-' }}</span>
        </div>
        <div class="sys-row">
          <span class="sys-label">当前角色</span>
          <span class="sys-value">{{ userStore.displayRoles?.join(', ') || '-' }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'
import { ElMessage } from 'element-plus'
import {
  Document, SuccessFilled, WarningFilled, Clock,
  Cpu, Printer, List, TrendCharts, CaretTop, CaretBottom, Odometer
} from '@element-plus/icons-vue'
import EmptyState from '@/components/states/EmptyState.vue'
import LoadingState from '@/components/states/LoadingState.vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const dashboardError = ref<string | null>(null)
const alarmError = ref<string | null>(null)
const resolvingAlarmId = ref<number | null>(null)
const refreshInFlight = ref(false)

function defaultKpis() {
  return [
    { label: '总任务数', value: 0, sub: '今日任务', trend: 0, icon: Document, type: 'primary' as const },
    { label: '已完成', value: 0, sub: '占比 0%', trend: 0, icon: SuccessFilled, type: 'success' as const },
    { label: '进行中', value: 0, sub: '占比 0%', trend: 0, icon: Clock, type: 'warning' as const },
    { label: '正在报警', value: 0, sub: '需干预', trend: 0, icon: WarningFilled, type: 'danger' as const }
  ]
}

const kpis = ref(defaultKpis())

const taskDist = ref<any[]>([])
const deviceDist = ref<any[]>([])
const alarmList = ref<any[]>([])
const alarmTotal = ref(0)
const alarmLoading = ref(false)

const maxTaskCount = computed(() => Math.max(1, ...taskDist.value.map((d: any) => d.count || 0)))
const maxDeviceCount = computed(() => Math.max(1, ...deviceDist.value.map((d: any) => d.count || 0)))

const quickActions = computed(() => {
  const actions = [
    { label: '任务管理', path: '/tasks', icon: List, bg: 'var(--ygt-primary-500)', perm: 'prod:task:view' },
    { label: '设备管理', path: '/devices', icon: Cpu, bg: 'var(--ygt-success)', perm: 'eq:device:list' },
    { label: '打印管理', path: '/print-center', icon: Printer, bg: 'var(--ygt-warning)', perm: 'prt:queue:view' },
    { label: '产能报表', path: '/capacity', icon: TrendCharts, bg: 'var(--ygt-info)', perm: 'ops:capacity:view' },
    { label: '温度曲线', path: '/temperature-curve', icon: Odometer, bg: '#e91e63', perm: 'eq:temp:view' },
  ]
  return actions.filter(a => userStore.hasPermission(a.perm))
})

function goQuick(path: string) {
  router.push(path)
}

function getRejectMessage(e: unknown): string {
  const x = e as Record<string, unknown> | undefined
  if (x && typeof x.message === 'string' && x.message) return x.message
  const err = e as { response?: { data?: { message?: string } } }
  const m = err?.response?.data?.message
  return typeof m === 'string' && m ? m : '加载失败，请稍后重试'
}

function alarmDisplayText(alarm: { content?: string; alarmType?: string }) {
  return alarm.content || alarm.alarmType || '-'
}

async function refreshAll() {
  if (refreshInFlight.value) return
  refreshInFlight.value = true
  dashboardError.value = null
  alarmError.value = null
  try {
    await Promise.all([fetchDashboard(), fetchAlarms()])
  } finally {
    refreshInFlight.value = false
  }
}

function safeInt(v: unknown, min = 0): number {
  const n = Math.floor(Number(v))
  if (!Number.isFinite(n)) return min
  return Math.max(min, n)
}

function safeTrend(v: unknown): number {
  const n = Number(v)
  if (!Number.isFinite(n)) return 0
  return n
}

/** 后端异常数据：在线数大于总数、告警数大于在线等，统一钳位避免条带与数字矛盾 */
function normalizeDeviceDistribution(raw: any[]) {
  return (raw || []).map((d: any) => {
    let count = safeInt(d?.count, 0)
    let online = safeInt(d?.onlineCount ?? d?.online_count, 0)
    let alarmN = safeInt(d?.onlineWithAlarmCount ?? d?.online_with_alarm_count, 0)
    online = Math.min(online, count)
    alarmN = Math.min(alarmN, online)
    const flexOnlineNormal = Math.max(0, online - alarmN)
    const flexAlarm = Math.max(0, alarmN)
    const flexOffline = Math.max(0, count - online)
    return {
      deviceType: String(d?.deviceType ?? d?.device_type ?? ''),
      count,
      onlineCount: online,
      onlineWithAlarmCount: alarmN,
      flexOnlineNormal,
      flexAlarm,
      flexOffline
    }
  })
}

function alarmItemClass(alarm: { alarmLevel?: string }) {
  const raw = (alarm.alarmLevel || 'info').toLowerCase()
  if (raw === 'critical' || raw === '严重') return 'alarm-critical'
  if (raw === 'warning' || raw === '警告') return 'alarm-warning'
  return 'alarm-info'
}

const statusMap: Record<string, { label: string; color: string }> = {
  '待泡药': { label: '待泡药', color: 'var(--ygt-gray-400)' },
  '泡药中': { label: '泡药中', color: 'var(--ygt-primary-500)' },
  '待煎药': { label: '待煎药', color: 'var(--ygt-info)' },
  '煎药中': { label: '煎药中', color: 'var(--ygt-warning)' },
  '待出液': { label: '待出液', color: 'var(--ygt-gray-400)' },
  '出液中': { label: '出液中', color: 'var(--ygt-info)' },
  '待包装': { label: '待包装', color: 'var(--ygt-gray-400)' },
  '包装中': { label: '包装中', color: 'var(--ygt-herb-500)' },
  '待贴标': { label: '待贴标', color: 'var(--ygt-gray-400)' },
  '待质检': { label: '待质检', color: 'var(--ygt-primary-500)' },
  '待交接': { label: '待交接', color: 'var(--ygt-success)' },
  '已完成': { label: '已完成', color: 'var(--ygt-success)' },
  '已部分完成': { label: '已部分完成', color: 'var(--ygt-success)' },
}

const deviceColorMap: Record<string, string> = {
  '1': 'var(--ygt-primary-500)',   // 煎药机
  '2': 'var(--ygt-success)',       // 包装机
  '3': 'var(--ygt-warning)',       // 标签打印机
  '4': 'var(--ygt-info)',          // 激光打印机
  '5': 'var(--ygt-herb-500)',      // PDA
}

function statusLabel(s: string) {
  return statusMap[s]?.label || s
}

function statusColor(s: string) {
  return statusMap[s]?.color || 'var(--ygt-gray-400)'
}

function formatDateTime(dt?: string) {
  if (!dt) return '-'
  const d = new Date(dt)
  if (isNaN(d.getTime())) return dt
  return d.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

function alarmLevelType(level?: string) {
  if (!level) return 'info'
  const l = level.toUpperCase()
  if (l === 'CRITICAL' || l === '严重') return 'danger'
  if (l === 'WARNING' || l === '警告') return 'warning'
  if (l === 'INFO' || l === '提示') return 'info'
  return 'info'
}

function deviceTypeName(type: string) {
  const t = String(type || '').trim()
  if (!t) return '未知类型'
  const map: Record<string, string> = { '1': '煎药机', '2': '包装机', '3': '标签打印机', '4': '激光打印机', '5': 'PDA' }
  return map[t] || `类型 ${t}`
}

function deviceColor(type: string) {
  return deviceColorMap[type] || 'var(--ygt-gray-400)'
}

function distPercent(value: number, max: number) {
  if (!max) return 0
  return Math.max(4, Math.round((value / max) * 100))
}

function exactPercent(value: number, max: number) {
  if (!max) return 0
  return Math.round((value / max) * 100)
}

async function fetchDashboard() {
  loading.value = true
  dashboardError.value = null
  try {
    const res: any = await request.get('/v1/ops/dashboard/realtime')
    const data = res?.data && typeof res.data === 'object' ? res.data : {}
    const totalTasks = safeInt(data.todayTotalTasks, 0)
    const ended = safeInt(data.todayEndedTasks, 0)
    const inProgress = safeInt(data.todayInProgressTasks, 0)
    const alerting = safeInt(data.todayAlertingTasks, 0)
    const endedRate = totalTasks > 0 ? Math.round((ended / totalTasks) * 100) : 0
    const inProgressRate = totalTasks > 0 ? Math.round((inProgress / totalTasks) * 100) : 0
    kpis.value = [
      { label: '总任务数', value: totalTasks, sub: '今日任务', trend: safeTrend(data.taskTrend), icon: Document, type: 'primary' },
      { label: '已完成', value: ended, sub: '占比 ' + endedRate + '%', trend: safeTrend(data.endedTrend), icon: SuccessFilled, type: 'success' },
      { label: '进行中', value: inProgress, sub: '占比 ' + inProgressRate + '%', trend: safeTrend(data.inProgressTrend), icon: Clock, type: 'warning' },
      { label: '正在报警', value: alerting, sub: '需干预', trend: safeTrend(data.alertingTrend), icon: WarningFilled, type: 'danger' }
    ]
    taskDist.value = (Array.isArray(data.taskStatusDistribution) ? data.taskStatusDistribution : []).map((d: any) => ({
      status: String(d?.status ?? '').trim() || '（未命名状态）',
      count: safeInt(d?.count, 0)
    }))
    deviceDist.value = normalizeDeviceDistribution(
      Array.isArray(data.deviceTypeDistribution) ? data.deviceTypeDistribution : []
    )
  } catch (e) {
    console.error('Dashboard fetch error:', e)
    dashboardError.value = getRejectMessage(e)
    kpis.value = defaultKpis()
    taskDist.value = []
    deviceDist.value = []
  } finally {
    loading.value = false
  }
}

async function fetchAlarms() {
  alarmLoading.value = true
  alarmError.value = null
  try {
    const res: any = await request.get('/v1/eq/alarms', { params: { status: 'PENDING', page: 1, size: 10 } })
    const page = res?.data && typeof res.data === 'object' ? res.data : {}
    const records = Array.isArray(page.records) ? page.records : []
    alarmList.value = records.filter((a: any) => a != null)
    alarmTotal.value = safeInt(page.total, alarmList.value.length)
  } catch (e) {
    console.error('Alarm fetch error:', e)
    alarmError.value = getRejectMessage(e)
    alarmList.value = []
    alarmTotal.value = 0
  } finally {
    alarmLoading.value = false
  }
}

async function resolveAlarm(id: number | undefined) {
  if (id == null || Number.isNaN(Number(id))) {
    ElMessage.warning('告警数据异常，无法处理')
    return
  }
  resolvingAlarmId.value = id
  try {
    await request.put(`/v1/eq/alarms/${id}/resolve`)
    ElMessage.success('告警已处理')
    await Promise.all([fetchAlarms(), fetchDashboard()])
  } catch (e) {
    // 错误提示由 request 拦截器统一弹出；此处仅结束 loading
    console.warn('resolveAlarm', e)
  } finally {
    resolvingAlarmId.value = null
  }
}

onMounted(() => {
  fetchDashboard()
  fetchAlarms()
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.dashboard-page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ygt-space-4);
  margin-bottom: var(--ygt-space-4);
}

.dashboard-alert {
  margin-bottom: var(--ygt-space-4);
}

.card-error {
  padding: var(--ygt-space-6);
  text-align: center;
  color: var(--ygt-text-secondary);
  font-size: var(--ygt-text-sm);
}
.card-error p {
  margin: 0 0 var(--ygt-space-3);
}

.device-legend {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ygt-space-4);
  font-size: var(--ygt-text-xs);
  color: var(--ygt-text-tertiary);
  margin-bottom: var(--ygt-space-3);
}
.device-legend > span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.legend-swatch {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 2px;
  flex-shrink: 0;
}
.legend-offline {
  background: var(--ygt-gray-200);
  border: 1px solid var(--ygt-gray-300);
}
.legend-online {
  background: var(--ygt-primary-400);
}
.legend-alarm {
  background: var(--el-color-danger);
}

.kpi-flat {
  color: var(--ygt-text-tertiary);
  font-weight: var(--ygt-fw-normal);
}

.page-title {
  font-size: var(--ygt-text-2xl);
  font-weight: var(--ygt-fw-semibold);
  color: var(--ygt-text-primary);
  margin-bottom: var(--ygt-space-6);
}

/* KPI 卡片 */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: var(--ygt-space-4);
  margin-bottom: var(--ygt-space-6);
}

.kpi-card {
  background: var(--ygt-bg-surface);
  border: 1px solid var(--ygt-border);
  border-radius: var(--ygt-radius-lg);
  padding: var(--ygt-space-5);
  position: relative;
  overflow: hidden;
  transition: box-shadow var(--ygt-duration-fast) var(--ygt-ease-out);
}
.kpi-card:hover {
  box-shadow: var(--ygt-shadow-md);
}
.kpi-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 3px;
}
.kpi-primary::before { background: var(--ygt-primary-500); }
.kpi-success::before { background: var(--ygt-success); }
.kpi-warning::before { background: var(--ygt-warning); }
.kpi-danger::before { background: var(--ygt-danger); }

.kpi-top {
  display: flex;
  align-items: center;
  gap: var(--ygt-space-2);
  margin-bottom: var(--ygt-space-3);
}
.kpi-icon {
  color: var(--ygt-text-tertiary);
}
.kpi-label {
  font-size: var(--ygt-text-sm);
  color: var(--ygt-text-secondary);
}

.kpi-value {
  font-size: var(--ygt-text-4xl);
  font-weight: var(--ygt-fw-bold);
  font-variant-numeric: tabular-nums;
  color: var(--ygt-text-primary);
  display: flex;
  align-items: baseline;
  gap: var(--ygt-space-2);
}

.kpi-trend {
  font-size: var(--ygt-text-sm);
  font-weight: var(--ygt-fw-medium);
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.kpi-trend.up { color: var(--ygt-success); }
.kpi-trend.down { color: var(--ygt-danger); }
.kpi-trend:not(.up):not(.down) { color: var(--ygt-text-tertiary); }

.kpi-sub {
  font-size: var(--ygt-text-xs);
  color: var(--ygt-text-tertiary);
  margin-top: var(--ygt-space-1);
}

/* 快捷入口 */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: var(--ygt-space-4);
  margin-bottom: var(--ygt-space-6);
}

.quick-item {
  background: var(--ygt-bg-surface);
  border: 1px solid var(--ygt-border);
  border-radius: var(--ygt-radius-lg);
  padding: var(--ygt-space-4);
  display: flex;
  align-items: center;
  gap: var(--ygt-space-3);
  cursor: pointer;
  transition: all var(--ygt-duration-fast) var(--ygt-ease-out);
}
.quick-item:hover {
  border-color: var(--ygt-primary-300);
  box-shadow: var(--ygt-shadow-md);
  transform: translateY(-1px);
}
.quick-item:focus-visible {
  outline: 2px solid var(--ygt-primary-500);
  outline-offset: 2px;
}

.quick-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--ygt-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.quick-label {
  font-size: var(--ygt-text-base);
  font-weight: var(--ygt-fw-medium);
  color: var(--ygt-text-primary);
}

/* 数据分布 */
.dashboard-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--ygt-space-4);
}

.dashboard-card {
  --el-card-padding: var(--ygt-space-5);
  border-radius: var(--ygt-radius-lg);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: var(--ygt-fw-semibold);
}

.dist-list {
  display: flex;
  flex-direction: column;
  gap: var(--ygt-space-3);
}

.dist-item {
  display: flex;
  align-items: center;
  gap: var(--ygt-space-3);
}

.dist-label {
  display: flex;
  align-items: center;
  gap: var(--ygt-space-2);
  width: 100px;
  flex-shrink: 0;
  font-size: var(--ygt-text-sm);
  color: var(--ygt-text-secondary);
}

.dist-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dist-bar-wrap {
  flex: 1;
  height: 8px;
  background: var(--ygt-gray-100);
  border-radius: var(--ygt-radius-full);
  overflow: hidden;
}

.dist-bar {
  height: 100%;
  border-radius: var(--ygt-radius-full);
  transition: width var(--ygt-duration-slow) var(--ygt-ease-out);
}

.dist-bar-stack {
  display: flex;
  height: 100%;
  width: 100%;
  border-radius: var(--ygt-radius-full);
  overflow: hidden;
}

.dist-bar-gray {
  height: 100%;
  background: var(--ygt-gray-100);
  transition: flex var(--ygt-duration-slow) var(--ygt-ease-out);
}

.dist-bar-online {
  height: 100%;
  transition: flex var(--ygt-duration-slow) var(--ygt-ease-out);
}

.dist-bar-alarm {
  height: 100%;
  background: var(--el-color-danger);
  transition: flex var(--ygt-duration-slow) var(--ygt-ease-out);
}

.dist-value {
  width: 80px;
  text-align: right;
  font-size: var(--ygt-text-sm);
  font-weight: var(--ygt-fw-semibold);
  color: var(--ygt-text-primary);
}

/* 异常预警列表 */
.alarm-list {
  display: flex;
  flex-direction: column;
  gap: var(--ygt-space-3);
}
.alarm-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ygt-space-3);
  padding: var(--ygt-space-3) var(--ygt-space-4);
  border-radius: var(--ygt-radius-md);
  border-left: 3px solid var(--ygt-info);
  background: var(--ygt-bg-surface);
  transition: background var(--ygt-duration-fast) var(--ygt-ease-out);
}
.alarm-item:hover {
  background: var(--ygt-gray-50);
}
.alarm-critical {
  border-left-color: var(--ygt-danger);
}
.alarm-warning {
  border-left-color: var(--ygt-warning);
}
.alarm-info {
  border-left-color: var(--ygt-info);
}
.alarm-main {
  flex: 1;
  min-width: 0;
}
.alarm-top {
  display: flex;
  align-items: center;
  gap: var(--ygt-space-2);
  margin-bottom: var(--ygt-space-1);
}
.alarm-device {
  font-size: var(--ygt-text-sm);
  font-weight: var(--ygt-fw-medium);
  color: var(--ygt-text-primary);
}
.alarm-time {
  font-size: var(--ygt-text-xs);
  color: var(--ygt-text-tertiary);
  margin-left: auto;
}
.alarm-content {
  font-size: var(--ygt-text-sm);
  color: var(--ygt-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.alarm-more {
  text-align: center;
  padding-top: var(--ygt-space-2);
}

/* 系统信息 */
.sys-info {
  display: flex;
  flex-direction: column;
  gap: var(--ygt-space-3);
}

.sys-row {
  display: flex;
  align-items: center;
  gap: var(--ygt-space-4);
}

.sys-label {
  width: 80px;
  font-size: var(--ygt-text-sm);
  color: var(--ygt-text-secondary);
  flex-shrink: 0;
}

.sys-value {
  font-size: var(--ygt-text-sm);
  color: var(--ygt-text-primary);
  font-weight: var(--ygt-fw-medium);
}

/* 移动端适配 */
@media (max-width: 768px) {
  .dashboard-row {
    grid-template-columns: 1fr;
  }
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .quick-actions {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
