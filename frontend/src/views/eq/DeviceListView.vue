<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <div>
            <el-button type="primary" v-if="userStore.hasPermission('eq:device:create')" @click="openDialog()">新增设备</el-button>
          </div>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="search.keyword" placeholder="设备编码/名称" clearable />
        </el-form-item>
        <el-form-item label="设备类型">
          <el-select v-model="search.deviceType" placeholder="全部" clearable style="width: 140px">
            <el-option label="煎药机" :value="1" />
            <el-option label="包装机" :value="2" />
            <el-option label="激光打印机" :value="3" />
            <el-option label="PDA" :value="4" />
            <el-option label="标签打印机" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="search.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="空闲" value="IDLE" />
            <el-option label="运行中" value="BUSY" />
            <el-option label="故障" value="FAULT" />
            <el-option label="维护中" value="MAINTENANCE" />
            <el-option label="离线" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属分组">
          <el-select v-model="search.groupId" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="g in groups" :key="g.id" :label="g.groupName" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="search.keyword = ''; search.deviceType = undefined; search.status = ''; search.groupId = undefined; fetchData()">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="deviceCode" label="设备编码" />
        <el-table-column prop="name" label="设备名称" />
        <el-table-column prop="deviceType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="deviceTypeTag(row.deviceType)">{{ deviceTypeText(row.deviceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="groupName" label="所属分组">
          <template #default="{ row }">
            <el-tag v-if="row.groupId && groupMap[row.groupId]" size="small">{{ groupMap[row.groupId] }}</el-tag>
            <el-tag v-else-if="row.groupName" size="small" type="info">{{ row.groupName }}</el-tag>
            <span v-else class="text-muted">未分组</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前温度" width="120">
          <template #default="{ row }">
            <span v-if="hasTemperature(row.deviceType)" class="ygt-num">
              {{ formatTemp(row.currentTemp) }}
            </span>
            <span v-else class="ygt-num" style="color: var(--ygt-gray-400)">-</span>
          </template>
        </el-table-column>
        <el-table-column label="最后心跳" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.lastHeartbeat) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="400" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="goToMonitor(row)">监控</el-button>
            <el-button size="small" type="default" v-if="userStore.hasPermission('eq:device:update')" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
            <el-button size="small" type="info" @click="openPrintLabel(row)">打印条码</el-button>
            <el-button size="small" type="danger" v-if="userStore.hasPermission('eq:device:delete')" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="fetchData"
      />
    </el-card>

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑设备' : '新增设备'" width="560px">
      <el-form :model="form" label-width="110px">
        <!-- 通用基础信息 -->
        <el-form-item label="设备编码" required>
          <el-input v-model="form.deviceCode" :disabled="!!form.id" placeholder="如 DECOCT_001" />
        </el-form-item>
        <el-form-item label="设备名称" required>
          <el-input v-model="form.name" placeholder="如 煎药机001" />
        </el-form-item>
        <el-form-item label="设备类型" required>
          <el-select v-model="form.deviceType" placeholder="请选择" style="width: 100%">
            <el-option label="煎药机" :value="1" />
            <el-option label="包装机" :value="2" />
            <el-option label="激光打印机" :value="3" />
            <el-option label="PDA" :value="4" />
            <el-option label="标签打印机" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属分组">
          <el-select v-model="form.groupId" placeholder="请选择" clearable style="width: 100%">
            <el-option v-for="g in groups" :key="g.id" :label="g.groupName" :value="g.id" />
          </el-select>
        </el-form-item>

        <!-- 通信配置（通用） -->
        <el-divider content-position="left">通信配置</el-divider>
        <el-form-item label="IP地址">
          <el-input v-model="form.ipAddress" placeholder="192.168.1.100" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        <el-form-item label="协议类型">
          <el-select v-model="form.protocolType" placeholder="请选择" clearable style="width: 100%">
            <el-option label="TCP" value="TCP" />
            <el-option label="MQTT" value="MQTT" />
            <el-option label="HTTP" value="HTTP" />
            <el-option label="Serial" value="SERIAL" />
          </el-select>
        </el-form-item>

        <!-- 煎药机专属 -->
        <template v-if="form.deviceType === 1">
          <el-divider content-position="left">煎药机参数</el-divider>
          <el-form-item label="煎煮模式">
            <el-select v-model="form.decoctMode" placeholder="请选择" clearable style="width: 100%">
              <el-option label="常压煎药" :value="1" />
              <el-option label="高压煎药" :value="2" />
              <el-option label="减压煎药" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="压力模式">
            <el-select v-model="form.pressureMode" placeholder="请选择" clearable style="width: 100%">
              <el-option label="无压力" :value="0" />
              <el-option label="低压" :value="1" />
              <el-option label="中压" :value="2" />
              <el-option label="高压" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="文火时间">
            <el-input-number v-model="form.slowFireTime" :min="0" :max="120" style="width: 100%" />
            <span style="margin-left: 8px; color: var(--ygt-text-tertiary)">分钟</span>
          </el-form-item>
          <el-form-item label="温度阈值">
            <el-input-number v-model="form.alarmMinTemp" :min="0" :max="200" :precision="1" placeholder="下限" />
            <span style="margin: 0 8px">~</span>
            <el-input-number v-model="form.alarmMaxTemp" :min="0" :max="200" :precision="1" placeholder="上限" />
            <span style="margin-left: 8px; color: var(--ygt-text-tertiary)">°C</span>
          </el-form-item>
        </template>

        <!-- 包装机专属 -->
        <template v-if="form.deviceType === 2">
          <el-divider content-position="left">包装机参数</el-divider>
          <el-form-item label="包装容量">
            <el-input-number v-model="form.packageCapacity" :min="50" :max="500" :step="10" style="width: 100%" />
            <span style="margin-left: 8px; color: var(--ygt-text-tertiary)">ml/袋</span>
          </el-form-item>
          <el-form-item label="温度阈值">
            <el-input-number v-model="form.alarmMaxTemp" :min="0" :max="200" :precision="1" />
            <span style="margin-left: 8px; color: var(--ygt-text-tertiary)">°C</span>
          </el-form-item>
        </template>

        <!-- PDA专属 -->
        <template v-if="form.deviceType === 4">
          <el-divider content-position="left">PDA参数</el-divider>
          <el-form-item label="通信ID">
            <el-input v-model="form.communicationId" placeholder="PDA设备通信识别码" />
          </el-form-item>
        </template>

        <!-- 标签打印机专属 -->
        <template v-if="form.deviceType === 5">
          <el-divider content-position="left">标签打印参数</el-divider>
          <el-form-item label="标签模式">
            <el-select v-model="form.labelMode" placeholder="请选择" clearable style="width: 100%">
              <el-option label="处方标签" value="PRESCRIPTION" />
              <el-option label="药品标签" value="MEDICINE" />
              <el-option label="物流标签" value="LOGISTICS" />
            </el-select>
          </el-form-item>
        </template>

        <!-- 资产信息（通用） -->
        <el-divider content-position="left">资产信息</el-divider>
        <el-form-item label="厂商">
          <el-input v-model="form.manufacturer" placeholder="如 东华原" />
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="form.modelNum" placeholder="如 YJD20" />
        </el-form-item>
        <el-form-item label="序列号">
          <el-input v-model="form.serialNumber" />
        </el-form-item>
        <el-form-item label="安装日期">
          <el-date-picker v-model="form.installDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="保修到期">
          <el-date-picker v-model="form.warrantyExpire" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="设备详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ detail?.id }}</el-descriptions-item>
        <el-descriptions-item label="编码">{{ detail?.deviceCode }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ detail?.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ deviceTypeText(detail?.deviceType) }}</el-descriptions-item>
        <el-descriptions-item label="分组">
          <el-tag v-if="detail?.groupId && groupMap[detail.groupId]" size="small">{{ groupMap[detail.groupId] }}</el-tag>
          <el-tag v-else-if="detail?.groupName" size="small" type="info">{{ detail.groupName }}</el-tag>
          <span v-else class="text-muted">未分组</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTag(detail?.status)">{{ statusText(detail?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="当前温度">
          <span v-if="hasTemperature(detail?.deviceType)" class="ygt-num">{{ formatTemp(detail?.currentTemp) }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="温度阈值">
          {{ detail?.alarmMaxTemp != null ? detail.alarmMaxTemp + ' °C' : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="最后心跳">{{ formatDateTime(detail?.lastHeartbeat) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail?.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 打印设备标签 -->
    <DeviceLabelPrint v-model="printVisible" :device="currentDevice" />

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import DeviceLabelPrint from '@/components/DeviceLabelPrint.vue'

const router = useRouter()

interface Device {
  id: number
  deviceCode: string
  name: string
  deviceType: number
  groupId?: number
  groupName?: string
  status: string
  currentTemp?: number
  alarmMaxTemp?: number
  alarmMinTemp?: number
  ipAddress?: string
  port?: number
  protocolType?: string
  decoctMode?: number
  pressureMode?: number
  slowFireTime?: number
  packageCapacity?: number
  packageNum?: number
  labelMode?: string
  communicationId?: string
  manufacturer?: string
  modelNum?: string
  serialNumber?: string
  installDate?: string
  warrantyExpire?: string
  detailStatus?: string
  lastHeartbeat?: string
  remark?: string
  createdAt?: string
}

interface Group {
  id: number
  groupName: string
}

const list = ref<Device[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const detail = ref<Device | null>(null)

// 标签打印
const printVisible = ref(false)
const currentDevice = ref<Device | null>(null)
const groups = ref<Group[]>([])
const groupMap = ref<Record<number, string>>({})

const search = ref({ keyword: '', deviceType: undefined as number | undefined, status: '', groupId: undefined as number | undefined })
const pagination = ref({ page: 1, size: 10, total: 0 })
const form = ref<Partial<Device>>({})

function deviceTypeText(type?: number) {
  const map: Record<number, string> = {
    1: '煎药机', 2: '包装机', 3: '激光打印机', 4: 'PDA', 5: '标签打印机'
  }
  return map[type || 0] || '未知'
}

function deviceTypeTag(type?: number) {
  if (type === 1) return 'primary'
  if (type === 2) return 'success'
  if (type === 3) return 'danger'
  if (type === 4) return 'warning'
  if (type === 5) return 'info'
  return 'info'
}

function statusText(status?: string) {
  const map: Record<string, string> = {
    'IDLE': '空闲', 'ONLINE': '空闲', 'OFFLINE': '离线',
    'FAULT': '故障', 'MAINTENANCE': '维护中', 'BUSY': '运行中'
  }
  return map[status || ''] || status || '未知'
}

function statusTag(status?: string) {
  if (status === 'IDLE' || status === 'ONLINE') return 'success'
  if (status === 'OFFLINE') return 'info'
  if (status === 'BUSY') return 'warning'
  if (status === 'MAINTENANCE') return 'warning'
  return 'danger'
}

function hasTemperature(deviceType?: number) {
  return deviceType === 1 || deviceType === 2
}

function formatTemp(temp?: number) {
  if (temp == null) return '-'
  return temp + ' °C'
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


async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.value.page,
      size: pagination.value.size,
      keyword: search.value.keyword || undefined,
      deviceType: search.value.deviceType || undefined,
      status: search.value.status || undefined,
      groupId: search.value.groupId || undefined
    }
    const res: any = await request.get('/v1/eq/devices', { params })
    list.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function openDialog(row?: Device) {
  form.value = row ? { ...row } : {}
  dialogVisible.value = true
}

async function handleSave() {
  try {
    if (form.value.id) {
      await request.put(`/v1/eq/devices/${form.value.id}`, form.value)
      ElMessage.success('更新成功')
    } else {
      await request.post('/v1/eq/devices', form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {}
}

async function handleDelete(row: Device) {
  try {
    await ElMessageBox.confirm('确认删除该设备？', '提示', { type: 'warning' })
    await request.delete(`/v1/eq/devices/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {}
}

async function viewDetail(row: Device) {
  try {
    const res: any = await request.get(`/v1/eq/devices/${row.id}`)
    detail.value = res.data
    detailVisible.value = true
  } catch (e) {}
}

function goToMonitor(row: Device) {
  router.push(`/device/${row.deviceCode}/detail`)
}

function openPrintLabel(row: Device) {
  currentDevice.value = row
  printVisible.value = true
}

async function fetchGroups() {
  try {
    const res: any = await request.get('/v1/eq/groups/all')
    const list = res.data || []
    groups.value = list
    const map: Record<number, string> = {}
    list.forEach((g: any) => { map[g.id] = g.groupName })
    groupMap.value = map
  } catch (e) {}
}

onMounted(() => {
  fetchData()
  fetchGroups()
})
</script>
