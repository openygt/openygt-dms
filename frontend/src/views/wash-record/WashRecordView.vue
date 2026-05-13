<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-form :inline="true" :model="query" @submit.prevent>
            <el-form-item label="设备编码">
              <el-input v-model="query.deviceCode" placeholder="设备编码" clearable style="width: 160px" />
            </el-form-item>
            <el-form-item label="清洗类型">
              <el-select v-model="query.washType" placeholder="全部" clearable style="width: 100px">
                <el-option label="常规" :value="1" />
                <el-option label="强化" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item label="结果">
              <el-select v-model="query.result" placeholder="全部" clearable style="width: 100px">
                <el-option label="合格" :value="1" />
                <el-option label="不合格" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="fetchList">查询</el-button>
              <el-button @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </template>
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column label="设备名称" width="140">
          <template #default="{ row }">{{ resolveDeviceName(row.deviceCode) }}</template>
        </el-table-column>
        <el-table-column prop="deviceCode" label="设备编码" width="120" />
        <el-table-column prop="washType" label="清洗类型" width="100">
          <template #default="{row}">
            <el-tag :type="row.washType === 2 ? 'danger' : 'info'">
              {{ row.washType === 2 ? '强化' : '常规' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="standardDuration" label="标准时长(分)" width="110" />
        <el-table-column prop="durationMin" label="实际时长(分)" width="110" />
        <el-table-column prop="result" label="结果" width="80">
          <template #default="{row}">
            <el-tag :type="row.result === 1 ? 'success' : row.result === 0 ? 'danger' : 'info'">
              {{ row.result === 1 ? '合格' : row.result === 0 ? '不合格' : '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <template #empty>
          <el-empty description="暂无清洗记录。清洗任务由生产调度自动下发，完成后自动生成记录。" />
        </template>
      </el-table>

      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="fetchList"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/api/request'
import { ElMessage } from 'element-plus'

const query = ref({ deviceCode: '', washType: null as number | null, result: null as number | null })
const list = ref([])
const loading = ref(false)
const pagination = ref({ page: 1, size: 20, total: 0 })
const deviceMap = ref<Record<string, string>>({})

function resolveDeviceName(code: string) {
  if (!code) return '-'
  return deviceMap.value[code] || code
}

async function loadDeviceMap() {
  try {
    const res: any = await request.get('/v1/eq/devices', { params: { page: 1, size: 200 } })
    const map: Record<string, string> = {}
    ;(res.data?.records || []).forEach((d: any) => { map[d.deviceCode] = d.name || d.deviceCode })
    deviceMap.value = map
  } catch (e) { console.warn('加载设备列表失败', e) }
}

const fetchList = async () => {
  loading.value = true
  try {
    const params: any = { page: pagination.value.page, size: pagination.value.size }
    if (query.value.deviceCode) params.deviceCode = query.value.deviceCode
    if (query.value.washType !== null) params.washType = query.value.washType
    if (query.value.result !== null) params.result = query.value.result
    const res = await request.get('/v1/eq/wash/list', { params })
    list.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } catch (e) {
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.value = { deviceCode: '', washType: null, result: null }
  pagination.value.page = 1
  fetchList()
}

onMounted(() => { loadDeviceMap(); fetchList() })
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
