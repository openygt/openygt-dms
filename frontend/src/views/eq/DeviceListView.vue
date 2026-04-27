<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>设备管理</span>
          <el-button type="primary" @click="openDialog()">新增设备</el-button>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="search.keyword" placeholder="设备编码/名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="search.keyword = ''; fetchData()">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="deviceCode" label="设备编码" />
        <el-table-column prop="deviceName" label="设备名称" />
        <el-table-column prop="deviceType" label="类型" width="120" />
        <el-table-column prop="groupName" label="所属分组" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ONLINE' ? 'success' : row.status === 'OFFLINE' ? 'info' : 'danger'">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentTemp" label="当前温度" width="100" />
        <el-table-column prop="lastHeartbeat" label="最后心跳" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
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
          <el-input v-model="form.deviceName" />
        </el-form-item>
        <el-form-item label="设备类型" required>
          <el-select v-model="form.deviceType" placeholder="请选择" style="width: 100%">
            <el-option label="煎药机" value="DECOCTER" />
            <el-option label="包装机" value="PACKER" />
            <el-option label="贴标机" value="LABELER" />
            <el-option label="泡药罐" value="SOAK_TANK" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属分组">
          <el-select v-model="form.groupId" placeholder="请选择" clearable style="width: 100%">
            <el-option v-for="g in groups" :key="g.id" :label="g.groupName" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="温度阈值">
          <el-input-number v-model="form.tempThreshold" :min="0" :max="200" />
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
        <el-descriptions-item label="名称">{{ detail?.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail?.deviceType }}</el-descriptions-item>
        <el-descriptions-item label="分组">{{ detail?.groupName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail?.status) }}</el-descriptions-item>
        <el-descriptions-item label="当前温度">{{ detail?.currentTemp }}</el-descriptions-item>
        <el-descriptions-item label="温度阈值">{{ detail?.tempThreshold }}</el-descriptions-item>
        <el-descriptions-item label="最后心跳">{{ detail?.lastHeartbeat }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail?.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

interface Device {
  id: number
  deviceCode: string
  deviceName: string
  deviceType: string
  groupId?: number
  groupName?: string
  status: string
  currentTemp?: number
  tempThreshold?: number
  lastHeartbeat?: string
  remark?: string
  createdAt: string
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
const groups = ref<Group[]>([])

const search = ref({ keyword: '' })
const pagination = ref({ page: 1, size: 10, total: 0 })
const form = ref<Partial<Device>>({})

function statusText(status?: string) {
  const map: Record<string, string> = {
    'ONLINE': '在线', 'OFFLINE': '离线', 'FAULT': '故障', 'MAINTENANCE': '维护中'
  }
  return map[status || ''] || status
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await request.get('/v1/eq/devices', {
      params: { page: pagination.value.page, size: pagination.value.size, keyword: search.value.keyword }
    })
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

async function fetchGroups() {
  try {
    // 设备分组接口暂未暴露，使用空数组
    const res: any = { data: { records: [] } }
    // const res: any = await request.get('/v1/eq/groups', { params: { page: 1, size: 999 } })
    groups.value = res.data?.records || []
  } catch (e) {}
}

onMounted(() => {
  fetchData()
  fetchGroups()
})
</script>
