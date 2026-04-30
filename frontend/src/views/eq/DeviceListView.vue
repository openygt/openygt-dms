<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>设备管理</span>
          <div>
            <el-button @click="pairingDialogVisible = true">生产线配对</el-button>
            <el-button type="primary" @click="openDialog()">新增设备</el-button>
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
            <el-option label="标签打印机" :value="3" />
            <el-option label="激光打印机" :value="4" />
            <el-option label="PDA" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="search.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="空闲" value="IDLE" />
            <el-option label="在线" value="ONLINE" />
            <el-option label="离线" value="OFFLINE" />
            <el-option label="故障" value="FAULT" />
            <el-option label="维护中" value="MAINTENANCE" />
            <el-option label="运行中" value="BUSY" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="search.keyword = ''; search.deviceType = undefined; search.status = ''; fetchData()">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="deviceCode" label="设备编码" />
        <el-table-column prop="name" label="设备名称" />
        <el-table-column prop="deviceType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="deviceTypeTag(row.deviceType)">{{ deviceTypeText(row.deviceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="groupName" label="所属分组">
          <template #default="{ row }">
            {{ pairingMap[row.id] || row.groupName || groupMap[row.groupId] || '-' }}
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
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
            <el-button size="small" type="info" @click="openPrintLabel(row)">标签</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑设备' : '新增设备'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="设备编码" required>
          <el-input v-model="form.deviceCode" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="设备名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="设备类型" required>
          <el-select v-model="form.deviceType" placeholder="请选择" style="width: 100%">
            <el-option label="煎药机" :value="1" />
            <el-option label="包装机" :value="2" />
            <el-option label="标签打印机" :value="3" />
            <el-option label="激光打印机" :value="4" />
            <el-option label="PDA" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属分组">
          <el-select v-model="form.groupId" placeholder="请选择" clearable style="width: 100%">
            <el-option v-for="g in groups" :key="g.id" :label="g.groupName" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="温度阈值">
          <el-input-number v-model="form.alarmMaxTemp" :min="0" :max="200" :precision="1" />
          <span style="margin-left: 8px; color: var(--ygt-text-tertiary)">°C</span>
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
        <el-descriptions-item label="分组">{{ detail?.groupName || (detail?.groupId != null ? groupMap[detail.groupId] : undefined) || '-' }}</el-descriptions-item>
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

    <!-- 生产线配对管理 -->
    <el-dialog v-model="pairingDialogVisible" title="生产线配对" width="800px">
      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="openPairingForm()">新增配对</el-button>
      </div>
      <el-table :data="pairingList" border>
        <el-table-column prop="pairingName" label="配对名称" />
        <el-table-column label="煎药机（最多4台）">
          <template #default="{ row }">
            <el-tag v-for="(d, idx) in row.decocters" :key="idx" size="small" style="margin-right: 4px">
              {{ d.name || d.deviceCode }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="包装机" width="140">
          <template #default="{ row }">
            {{ row.packer?.name || row.packer?.deviceCode || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="标签打印机" width="140">
          <template #default="{ row }">
            {{ row.labeler?.name || row.labeler?.deviceCode || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openPairingForm(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDeletePairing(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!pairingLoading && pairingList.length === 0" description="暂无配对记录" />
    </el-dialog>

    <!-- 配对表单 -->
    <el-dialog v-model="pairingFormVisible" :title="pairingForm.id ? '编辑配对' : '新增配对'" width="600px">
      <el-form :model="pairingForm" label-width="120px">
        <el-form-item label="配对名称" required>
          <el-input v-model="pairingForm.pairingName" placeholder="如：生产线A" />
        </el-form-item>
        <el-form-item label="煎药机" required>
          <el-select v-model="pairingForm.decocterIds" multiple :multiple-limit="4" placeholder="请选择1-4台煎药机" style="width: 100%">
            <el-option v-for="d in availableDecocters" :key="d.id" :label="d.name || d.deviceCode" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="包装机" required>
          <el-select v-model="pairingForm.packerId" placeholder="请选择包装机" style="width: 100%">
            <el-option v-for="d in availablePackers" :key="d.id" :label="d.name || d.deviceCode" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签打印机" required>
          <el-select v-model="pairingForm.labelerId" placeholder="请选择标签打印机" style="width: 100%">
            <el-option v-for="d in availableLabelers" :key="d.id" :label="d.name || d.deviceCode" :value="d.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pairingFormVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePairing">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import DeviceLabelPrint from '@/components/DeviceLabelPrint.vue'

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
  lastHeartbeat?: string
  remark?: string
  createdAt?: string
}

interface Group {
  id: number
  groupName: string
}

interface Pairing {
  id?: number
  pairingName: string
  decocterIds: number[]
  packerId: number
  labelerId: number
  decocters?: Device[]
  packer?: Device
  labeler?: Device
}

const list = ref<Device[]>([])
const allDevices = ref<Device[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const detail = ref<Device | null>(null)

// 标签打印
const printVisible = ref(false)
const currentDevice = ref<Device | null>(null)
const groups = ref<Group[]>([])
const groupMap = ref<Record<number, string>>({})

const search = ref({ keyword: '', deviceType: undefined as number | undefined, status: '' })
const pagination = ref({ page: 1, size: 10, total: 0 })
const form = ref<Partial<Device>>({})

// 配对相关
const pairingDialogVisible = ref(false)
const pairingFormVisible = ref(false)
const pairingList = ref<Pairing[]>([])
const pairingLoading = ref(false)
const pairingForm = ref<Partial<Pairing>>({ decocterIds: [] })

// 已配对设备ID集合（排除当前编辑的配对组）
const pairedDeviceIds = computed(() => {
  const ids = new Set<number>()
  const currentId = pairingForm.value.id
  pairingList.value.forEach(p => {
    if (currentId && p.id === currentId) return // 当前编辑组中的设备仍可选
    p.decocterIds?.forEach((id: number) => ids.add(id))
    if (p.packerId) ids.add(p.packerId)
    if (p.labelerId) ids.add(p.labelerId)
  })
  return ids
})

// 全部设备按类型
const allDecocters = computed(() => allDevices.value.filter(d => d.deviceType === 1))
const allPackers = computed(() => allDevices.value.filter(d => d.deviceType === 2))
const allLabelers = computed(() => allDevices.value.filter(d => d.deviceType === 3))

// 尚未配对的设备（当前编辑组中的除外）
const availableDecocters = computed(() => {
  const currentIds = new Set(pairingForm.value.decocterIds || [])
  return allDecocters.value.filter(d => !pairedDeviceIds.value.has(d.id) || currentIds.has(d.id))
})
const availablePackers = computed(() => {
  const currentId = pairingForm.value.packerId
  return allPackers.value.filter(d => !pairedDeviceIds.value.has(d.id) || d.id === currentId)
})
const availableLabelers = computed(() => {
  const currentId = pairingForm.value.labelerId
  return allLabelers.value.filter(d => !pairedDeviceIds.value.has(d.id) || d.id === currentId)
})

function deviceTypeText(type?: number) {
  const map: Record<number, string> = {
    1: '煎药机', 2: '包装机', 3: '标签打印机', 4: '激光打印机', 5: 'PDA'
  }
  return map[type || 0] || '未知'
}

function deviceTypeTag(type?: number) {
  if (type === 1) return 'primary'
  if (type === 2) return 'success'
  if (type === 3) return 'warning'
  if (type === 4) return 'danger'
  return 'info'
}

function statusText(status?: string) {
  const map: Record<string, string> = {
    'IDLE': '空闲', 'ONLINE': '在线', 'OFFLINE': '离线',
    'FAULT': '故障', 'MAINTENANCE': '维护中', 'BUSY': '运行中'
  }
  return map[status || ''] || status || '未知'
}

function statusTag(status?: string) {
  if (status === 'ONLINE' || status === 'IDLE') return 'success'
  if (status === 'OFFLINE') return 'info'
  if (status === 'BUSY') return 'warning'
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

const pairingMap = ref<Record<number, string>>({})

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.value.page,
      size: pagination.value.size,
      keyword: search.value.keyword || undefined,
      deviceType: search.value.deviceType || undefined,
      status: search.value.status || undefined
    }
    const res: any = await request.get('/v1/eq/devices', { params })
    list.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
    // 缓存全部设备用于配对选择
    if (allDevices.value.length === 0) {
      await fetchAllDevices()
    }
    // 构建设备ID到配对名称的映射
    await buildPairingMap()
  } finally {
    loading.value = false
  }
}

async function fetchAllDevices() {
  try {
    const res: any = await request.get('/v1/eq/devices', { params: { page: 1, size: 999 } })
    allDevices.value = res.data?.records || []
  } catch (e) {}
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

function openPrintLabel(row: Device) {
  currentDevice.value = row
  printVisible.value = true
}

async function fetchGroups() {
  try {
    const res: any = await request.get('/v1/eq/groups/all')
    const list = res.data?.records || []
    groups.value = list
    const map: Record<number, string> = {}
    list.forEach((g: any) => { map[g.id] = g.groupName })
    groupMap.value = map
  } catch (e) {}
}

// 配对管理
async function fetchPairings() {
  pairingLoading.value = true
  try {
    const res: any = await request.get('/v1/eq/pairings')
    pairingList.value = res.data || []
  } catch (e) {
    // 后端可能还未实现，显示空列表
    pairingList.value = []
  } finally {
    pairingLoading.value = false
  }
}

function openPairingForm(row?: Pairing) {
  if (row) {
    // 将后端返回的 decocterIds 转为数字数组，确保 el-select 正确匹配
    const decocterIds = Array.isArray(row.decocterIds)
      ? row.decocterIds.map((id: any) => typeof id === 'string' ? parseInt(id, 10) : id)
      : []
    pairingForm.value = {
      id: row.id,
      pairingName: row.pairingName,
      decocterIds,
      packerId: row.packerId,
      labelerId: row.labelerId
    }
  } else {
    pairingForm.value = { pairingName: '', decocterIds: [], packerId: undefined, labelerId: undefined }
  }
  pairingFormVisible.value = true
}

async function handleSavePairing() {
  try {
    const data = pairingForm.value
    if (!data.pairingName || !data.decocterIds?.length || !data.packerId || !data.labelerId) {
      ElMessage.warning('请填写完整的配对信息')
      return
    }
    // decocterIds 前端是数组，后端期望逗号分隔字符串
    const payload = {
      pairingName: data.pairingName,
      decocterIds: Array.isArray(data.decocterIds) ? data.decocterIds.join(',') : data.decocterIds,
      packerId: data.packerId,
      labelerId: data.labelerId
    }
    if (data.id) {
      await request.put(`/v1/eq/pairings/${data.id}`, payload)
      ElMessage.success('更新成功')
    } else {
      await request.post('/v1/eq/pairings', payload)
      ElMessage.success('创建成功')
    }
    pairingFormVisible.value = false
    fetchPairings()
  } catch (e) {}
}

async function handleDeletePairing(row: Pairing) {
  try {
    await ElMessageBox.confirm('确认删除该配对？', '提示', { type: 'warning' })
    await request.delete(`/v1/eq/pairings/${row.id}`)
    ElMessage.success('删除成功')
    fetchPairings()
    fetchData()
  } catch (e) {}
}

async function buildPairingMap() {
  try {
    const res: any = await request.get('/v1/eq/pairings')
    const pairings: Pairing[] = res.data || []
    const map: Record<number, string> = {}
    pairings.forEach((p: Pairing) => {
      p.decocterIds?.forEach((id: number) => {
        map[id] = p.pairingName
      })
      if (p.packerId) map[p.packerId] = p.pairingName
      if (p.labelerId) map[p.labelerId] = p.pairingName
    })
    pairingMap.value = map
    // 强制刷新列表触发 el-table 重新渲染
    list.value = [...list.value]
  } catch (e) {
    pairingMap.value = {}
  }
}

watch(pairingDialogVisible, (val) => {
  if (val) fetchPairings()
})

onMounted(() => {
  fetchData()
  fetchGroups()
})
</script>
