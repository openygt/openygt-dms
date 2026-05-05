<template>
  <div class="dashboard">
    <div class="page-header-title">生产看板：<span class="page-header-sub">今日任务数、进行中、已完成、设备在线率、告警数</span></div>

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
        <div class="kpi-sub">{{ kpi.sub }}</div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="quick-actions">
      <div
        v-for="action in quickActions"
        :key="action.path"
        class="quick-item"
        @click="$router.push(action.path)"
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
        <EmptyState
          v-else-if="!taskDist.length"
          title="暂无任务数据"
          description="今日还没有生成任务"
        />
        <div v-else class="dist-list">
          <div v-for="item in taskDist" :key="item.status" class="dist-item">
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
              <el-tag size="small" type="info">总数/在线/在线有异常</el-tag>
              <el-button size="small" type="primary" style="margin-left: 8px" @click="$router.push('/device-monitor')">进入监控</el-button>
            </div>
          </div>
        </template>
        <LoadingState v-if="loading" description="加载设备数据..." />
        <EmptyState
          v-else-if="!deviceDist.length"
          title="暂无设备数据"
          description="还没有录入设备信息"
        />
        <div v-else class="dist-list">
          <div v-for="item in deviceDist" :key="item.deviceType" class="dist-item">
            <div class="dist-label">
              <span class="dist-dot" :style="{ background: deviceColor(item.deviceType) }"></span>
              <span>{{ deviceTypeName(item.deviceType) }}</span>
            </div>
            <div class="dist-bar-wrap" :style="{ width: exactPercent(item.count, maxDeviceCount) + '%' }">
              <div class="dist-bar-stack">
                <div class="dist-bar-online" :style="{ flex: (item.onlineCount || 0) - (item.onlineWithAlarmCount || 0), background: deviceColor(item.deviceType) }"></div>
                <div class="dist-bar-alarm" :style="{ flex: item.onlineWithAlarmCount || 0 }"></div>
                <div class="dist-bar-gray" :style="{ flex: item.count - (item.onlineCount || 0) }"></div>
              </div>
            </div>
            <span class="dist-value ygt-num">
              {{ item.count }}/{{ item.onlineCount||0 }}/{{ item.onlineWithAlarmCount||0 }}
            </span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 异常预警列表 -->
    <el-card shadow="never" style="margin-top: var(--ygt-space-4)">
      <template #header>
        <div class="card-header">
          <span>异常预警</span>
          <div>
            <el-tag v-if="alarmList.length > 0" size="small" type="danger">{{ alarmList.length }} 条待处理</el-tag>
            <el-tag v-else size="small" type="success">无异常</el-tag>
            <el-button size="small" type="primary" style="margin-left: 8px" @click="$router.push('/alarms')">查看全部</el-button>
          </div>
        </div>
      </template>
      <LoadingState v-if="alarmLoading" description="加载告警数据..." />
      <EmptyState
        v-else-if="alarmList.length === 0"
        title="暂无异常预警"
        description="系统运行正常，未检测到异常"
      />
      <div v-else class="alarm-list">
        <div v-for="alarm in alarmList.slice(0, 5)" :key="alarm.id" class="alarm-item" :class="`alarm-${alarm.alarmLevel?.toLowerCase() || 'info'}`">
          <div class="alarm-main">
            <div class="alarm-top">
              <el-tag size="small" :type="alarmLevelType(alarm.alarmLevel)">{{ alarm.alarmLevel || 'INFO' }}</el-tag>
              <span class="alarm-device">{{ alarm.deviceCode || '-' }}</span>
              <span class="alarm-time">{{ formatDateTime(alarm.createdAt) }}</span>
            </div>
            <div class="alarm-content">{{ alarm.content || alarm.alarmType || '-' }}</div>
          </div>
          <el-button size="small" type="success" @click="resolveAlarm(alarm.id)">处理</el-button>
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
import { useUserStore } from '@/stores/user'
import request from '@/api/request'
import { ElMessage } from 'element-plus'
import {
  Document, SuccessFilled, WarningFilled, Clock,
  Pointer, Cpu, Printer, List, TrendCharts, CaretTop, CaretBottom, Minus
} from '@element-plus/icons-vue'
import EmptyState from '@/components/states/EmptyState.vue'
import LoadingState from '@/components/states/LoadingState.vue'

const userStore = useUserStore()
const loading = ref(false)

const kpis = ref([
  { label: '今日任务', value: 0, sub: '较昨日', trend: 0, icon: 'Document', type: 'primary' },
  { label: '已完成', value: 0, sub: '完成率', trend: 0, icon: 'SuccessFilled', type: 'success' },
  { label: '进行中', value: 0, sub: '活跃工单', trend: 0, icon: 'Clock', type: 'warning' },
  { label: '活跃告警', value: 0, sub: '待处理', trend: 0, icon: 'WarningFilled', type: 'danger' }
])

const taskDist = ref<any[]>([])
const deviceDist = ref<any[]>([])
const alarmList = ref<any[]>([])
const alarmTotal = ref(0)
const alarmLoading = ref(false)

const maxTaskCount = computed(() => Math.max(1, ...taskDist.value.map((d: any) => d.count || 0)))
const maxDeviceCount = computed(() => Math.max(1, ...deviceDist.value.map((d: any) => d.count || 0)))

const quickActions = computed(() => {
  const actions = [
    { label: '任务管理', path: '/tasks', icon: 'List', bg: 'var(--ygt-primary-500)', perm: 'prod:task:view' },
    { label: '设备管理', path: '/devices', icon: 'Cpu', bg: 'var(--ygt-success)', perm: 'eq:device:list' },
    { label: '打印管理', path: '/print-center', icon: 'Printer', bg: 'var(--ygt-warning)', perm: 'prt:queue:view' },
    { label: '产能报表', path: '/capacity', icon: 'TrendCharts', bg: 'var(--ygt-info)', perm: 'ops:capacity:view' },
    { label: '温度曲线', path: '/temperature-curve', icon: 'Odometer', bg: '#e91e63', perm: 'eq:device:monitor' },
  ]
  return actions.filter(a => userStore.hasPermission(a.perm))
})

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
  const map: Record<string, string> = { '1': '煎药机', '2': '包装机', '3': '标签打印机', '4': '激光打印机', '5': 'PDA' }
  return map[type] || type
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
  try {
    const res: any = await request.get('/v1/ops/dashboard/realtime')
    const data = res.data || {}
    const totalTasks = data.todayTotalTasks || 0
    const ended = data.todayEndedTasks || 0
    const inProgress = data.todayInProgressTasks || 0
    const alerting = data.todayAlertingTasks || 0
    // 占比计算（避免除零）
    const endedRate = totalTasks > 0 ? Math.round((ended / totalTasks) * 100) : 0
    const inProgressRate = totalTasks > 0 ? Math.round((inProgress / totalTasks) * 100) : 0
    kpis.value = [
      { label: '总任务数', value: totalTasks, sub: '今日任务', trend: data.taskTrend || 0, icon: 'Document', type: 'primary' },
      { label: '已完成', value: ended, sub: '占比 ' + endedRate + '%', trend: data.endedTrend || 0, icon: 'SuccessFilled', type: 'success' },
      { label: '进行中', value: inProgress, sub: '占比 ' + inProgressRate + '%', trend: data.inProgressTrend || 0, icon: 'Clock', type: 'warning' },
      { label: '正在报警', value: alerting, sub: '需干预', trend: data.alertingTrend || 0, icon: 'WarningFilled', type: 'danger' }
    ]
    taskDist.value = (data.taskStatusDistribution || []).map((d: any) => ({ status: d.status || d.status, count: d.count || 0 }))
    deviceDist.value = (data.deviceTypeDistribution || []).map((d: any) => ({
      deviceType: d.deviceType || d.device_type,
      count: d.count || 0,
      onlineCount: d.onlineCount || d.onlineCount || 0,
      onlineWithAlarmCount: d.onlineWithAlarmCount || d.onlineWithAlarmCount || 0
    }))
  } catch (e) {
    console.error('Dashboard fetch error:', e)
  } finally {
    loading.value = false
  }
}

async function fetchAlarms() {
  alarmLoading.value = true
  try {
    const res: any = await request.get('/v1/eq/alarms', { params: { status: 'PENDING', page: 1, size: 10 } })
    const page = res.data || {}
    alarmList.value = page.records || []
    alarmTotal.value = page.total || 0
  } catch (e) {
    console.error('Alarm fetch error:', e)
  } finally {
    alarmLoading.value = false
  }
}

async function resolveAlarm(id: number) {
  try {
    await request.put(`/v1/eq/alarms/${id}/resolve`)
    ElMessage.success('告警已处理')
    fetchAlarms()
    fetchDashboard()
  } catch (e) {
    ElMessage.error('处理失败')
  }
}

onMounted(() => {
  fetchDashboard()
  fetchAlarms()
})
</script>

<style scoped>
.dashboard {
  padding: var(--ygt-space-4);
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
