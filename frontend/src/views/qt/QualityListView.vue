<template>
  <div>
    <!-- 待质检任务 -->
    <el-card style="margin-bottom: 16px">
      <template #header>
        <span>待质检任务</span>
      </template>
      <el-table :data="pendingList" v-loading="pendingLoading" border>
        <el-table-column prop="id" label="任务号" width="100" />
        <el-table-column prop="barcode" label="条码" />
        <el-table-column prop="status" label="当前状态" width="120" />
        <el-table-column prop="operatorId" label="操作人" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="openInspectDialog(row, 'PASS')">通过</el-button>
            <el-button size="small" type="danger" @click="openInspectDialog(row, 'CONCESSION')">不通过</el-button>
            <el-button size="small" type="warning" @click="openReworkDialog(row)">返工</el-button>
            <el-button size="small" type="primary" @click="openDetailInspectDialog(row)">详细质检</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!pendingLoading && pendingList.length === 0" description="暂无待质检任务" />
    </el-card>

    <!-- 质检记录 -->
    <el-card>
      <template #header>
        <span>质检记录</span>
      </template>
      <el-form :inline="true" :model="query" style="margin-bottom: 16px">
        <el-form-item label="质检结果">
          <el-select v-model="query.result" placeholder="全部" clearable style="width: 140px">
            <el-option label="通过" value="PASS" />
            <el-option label="不通过" value="CONCESSION" />
            <el-option label="返工" value="REWORK" />
          </el-select>
        </el-form-item>
        <el-form-item label="起止时间">
          <el-date-picker
            v-model="query.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 340px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="taskId" label="任务号" width="100" />
        <el-table-column prop="result" label="结果" width="120">
          <template #default="{ row }">
            <el-tag :type="resultType(row.result)">{{ resultText(row.result) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorId" label="质检员" />
        <el-table-column prop="remark" label="备注" />
        <el-table-column prop="createdAt" label="质检时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
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

    <!-- 通用质检弹窗（通过/不通过） -->
    <el-dialog v-model="inspectVisible" :title="inspectTitle" width="400px">
      <el-form :model="inspectForm" label-width="80px">
        <el-form-item label="任务号">
          <span>{{ inspectForm.taskId }}</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="inspectForm.remark" type="textarea" rows="3" placeholder="请输入备注..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inspectVisible = false">取消</el-button>
        <el-button type="primary" @click="handleInspectSubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- 返工弹窗 -->
    <el-dialog v-model="reworkVisible" title="返工处理" width="400px">
      <el-form :model="reworkForm" label-width="100px">
        <el-form-item label="任务号">
          <span>{{ reworkForm.taskId }}</span>
        </el-form-item>
        <el-form-item label="返工节点" required>
          <el-select v-model="reworkForm.reworkNode" placeholder="请选择返工节点" style="width: 100%" @change="onReworkNodeChange">
            <el-option
              v-for="node in reworkNodes"
              :key="node.value"
              :label="node.label"
              :value="node.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="reworkForm.remark" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reworkVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReworkSubmit">确认返工</el-button>
      </template>
    </el-dialog>

    <!-- 详细质检弹窗 -->
    <el-dialog v-model="detailInspectVisible" title="详细质检执行" width="640px">
      <el-form :model="detailInspectForm" label-width="100px">
        <el-form-item label="任务号">
          <span>{{ detailInspectForm.taskId }}</span>
        </el-form-item>
        <el-form-item label="总体结果">
          <el-radio-group v-model="detailInspectForm.overallResult">
            <el-radio label="PASS">通过</el-radio>
            <el-radio label="CONCESSION">不通过</el-radio>
            <el-radio label="REWORK">返工</el-radio>
            <el-radio label="SCRAP">报废</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="detailInspectForm.overallResult === 'REWORK'" label="返工节点" required>
          <el-select v-model="detailInspectForm.reworkNode" placeholder="请选择返工节点" style="width: 100%">
            <el-option label="待泡药" value="待泡药" />
            <el-option label="待煎药" value="待煎药" />
            <el-option label="待出液" value="待出液" />
            <el-option label="待包装" value="待包装" />
            <el-option label="待贴标" value="待贴标" />
          </el-select>
        </el-form-item>

        <el-divider>检查项明细</el-divider>

        <div v-for="(item, idx) in detailInspectForm.items" :key="idx" class="inspect-item-row">
          <div class="inspect-item-header">
            <span class="inspect-item-name">{{ item.itemName }}</span>
            <el-radio-group v-model="item.result" size="small">
              <el-radio label="PASS">通过</el-radio>
              <el-radio label="FAIL">不通过</el-radio>
              <el-radio label="NA">不适用</el-radio>
            </el-radio-group>
          </div>
          <el-input v-if="item.itemCode === 'DOSE'" v-model="item.actualValue" placeholder="实际值（如 200ml）" size="small" style="width: 200px; margin-top: 4px" />
          <el-input v-model="item.remark" placeholder="备注" size="small" style="margin-top: 4px" />
        </div>

        <el-form-item label="总体备注" style="margin-top: 16px">
          <el-input v-model="detailInspectForm.remark" type="textarea" rows="3" placeholder="请输入总体备注..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="detailInspectVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDetailInspectSubmit">提交质检</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

interface QcRecord {
  id: number
  taskId: number
  result: string
  operatorId: string
  remark: string
  createdAt: string
}

interface Task {
  id: number
  barcode: string
  status: string
  operatorId: string
  createdAt: string
}

const list = ref<QcRecord[]>([])
const loading = ref(false)
const pendingList = ref<Task[]>([])
const pendingLoading = ref(false)

const query = ref({
  result: '',
  dateRange: [] as string[]
})
const pagination = ref({ page: 1, size: 20, total: 0 })

// 通用质检弹窗（通过/不通过）
const inspectVisible = ref(false)
const inspectForm = ref({ taskId: 0, result: '', remark: '' })
const inspectTitle = computed(() => {
  if (inspectForm.value.result === 'PASS') return '质检通过'
  if (inspectForm.value.result === 'CONCESSION') return '质检不通过'
  return '质检'
})

// 返工弹窗
const reworkVisible = ref(false)
const reworkForm = ref({ taskId: 0, reworkNode: '', remark: '' })
const reworkNodes = ref<{ label: string; value: string }[]>([])

// 详细质检弹窗
const detailInspectVisible = ref(false)
const detailInspectForm = ref({
  taskId: 0,
  overallResult: 'PASS',
  reworkNode: '',
  remark: '',
  items: [] as { itemCode: string; itemName: string; result: string; actualValue: string; remark: string }[]
})

const defaultInspectItems = [
  { itemCode: 'APPEARANCE', itemName: '外观检查', result: 'PASS', actualValue: '', remark: '' },
  { itemCode: 'ODOR', itemName: '气味检查', result: 'PASS', actualValue: '', remark: '' },
  { itemCode: 'DOSE', itemName: '剂量检查', result: 'PASS', actualValue: '', remark: '' },
  { itemCode: 'SEAL', itemName: '密封检查', result: 'PASS', actualValue: '', remark: '' },
  { itemCode: 'LABEL', itemName: '标签核对', result: 'PASS', actualValue: '', remark: '' }
]

function resultText(result?: string) {
  const map: Record<string, string> = {
    'PASS': '通过',
    'CONCESSION': '不通过',
    'REWORK': '返工'
  }
  return map[result || ''] || result || '-'
}

function resultType(result?: string) {
  if (result === 'PASS') return 'success'
  if (result === 'CONCESSION') return 'danger'
  if (result === 'REWORK') return 'warning'
  return ''
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

// 质检记录列表
async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.value.page,
      size: pagination.value.size
    }
    if (query.value.result) params.result = query.value.result
    if (query.value.dateRange && query.value.dateRange.length === 2) {
      params.startTime = query.value.dateRange[0]
      params.endTime = query.value.dateRange[1]
    }
    const res: any = await request.get('/v1/qt/inspections', { params })
    list.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.value.page = 1
  fetchData()
}

function handleReset() {
  query.value = { result: '', dateRange: [] }
  pagination.value.page = 1
  fetchData()
}

// 待质检任务
async function fetchPendingTasks() {
  pendingLoading.value = true
  try {
    const res: any = await request.get('/v1/prod/tasks', {
      params: { status: '待质检', page: 1, size: 100 }
    })
    pendingList.value = res.data?.records || []
  } catch (e) {
    pendingList.value = []
  } finally {
    pendingLoading.value = false
  }
}

// 打开通用质检弹窗（通过/不通过）
function openInspectDialog(row: Task, result: string) {
  inspectForm.value = { taskId: row.id, result, remark: '' }
  inspectVisible.value = true
}

// 提交通用质检（通过/不通过）
async function handleInspectSubmit() {
  try {
    const { taskId, result, remark } = inspectForm.value
    // 1. 更新生产任务状态
    await request.post(`/v1/prod/tasks/${taskId}/quality`, {
      result,
      operatorId: 'admin',
      remark
    })
    // 2. 记录质检结果
    await request.post('/v1/qt/inspect', null, {
      params: { taskId, result, operatorId: 'admin', remark }
    })
    ElMessage.success(result === 'PASS' ? '质检已通过' : '质检已标记不通过')
    inspectVisible.value = false
    fetchPendingTasks()
    fetchData()
  } catch (e) {}
}

// 打开返工弹窗
async function openReworkDialog(row: Task) {
  reworkForm.value = { taskId: row.id, reworkNode: '', remark: '' }
  reworkNodes.value = []
  try {
    const res: any = await request.get(`/v1/prod/tasks/${row.id}/steps`)
    const steps: any[] = res.data || []
    const nodeSet = new Set<string>()
    const nodeMap: Record<string, string> = {
      'SOAK': '待泡药',
      'DECOCT': '待煎药',
      'POUR': '待出液',
      'WRAP': '待包装',
      'LABEL': '待贴标'
    }
    steps.forEach((s: any) => {
      const stepType = s.stepType
      if (nodeMap[stepType]) {
        nodeSet.add(stepType)
      }
    })
    if (nodeSet.size === 0) {
      Object.keys(nodeMap).forEach(k => nodeSet.add(k))
    }
    reworkNodes.value = Array.from(nodeSet).map(k => ({
      label: nodeMap[k],
      value: nodeMap[k]
    }))
  } catch (e) {
    reworkNodes.value = [
      { label: '待泡药', value: '待泡药' },
      { label: '待煎药', value: '待煎药' },
      { label: '待出液', value: '待出液' },
      { label: '待包装', value: '待包装' },
      { label: '待贴标', value: '待贴标' }
    ]
  }
  reworkVisible.value = true
}

// 返工节点选择变化时自动更新备注
function onReworkNodeChange(val: string) {
  if (val) {
    reworkForm.value.remark = `返工到${val}`
  }
}

// 提交返工
async function handleReworkSubmit() {
  if (!reworkForm.value.reworkNode) {
    ElMessage.warning('请选择返工节点')
    return
  }
  try {
    const { taskId, reworkNode, remark } = reworkForm.value
    // 1. 更新生产任务状态
    await request.post(`/v1/prod/tasks/${taskId}/quality`, {
      result: 'REWORK',
      operatorId: 'admin',
      remark,
      reworkNode
    })
    // 2. 记录质检结果
    await request.post('/v1/qt/inspect', null, {
      params: { taskId, result: 'REWORK', operatorId: 'admin', remark, reworkNode }
    })
    ElMessage.success('返工已提交')
    reworkVisible.value = false
    fetchPendingTasks()
    fetchData()
  } catch (e) {}
}

// 打开详细质检弹窗
function openDetailInspectDialog(row: Task) {
  detailInspectForm.value = {
    taskId: row.id,
    overallResult: 'PASS',
    reworkNode: '',
    remark: '',
    items: JSON.parse(JSON.stringify(defaultInspectItems))
  }
  detailInspectVisible.value = true
}

// 提交详细质检
async function handleDetailInspectSubmit() {
  const form = detailInspectForm.value
  if (form.overallResult === 'REWORK' && !form.reworkNode) {
    ElMessage.warning('请选择返工节点')
    return
  }
  try {
    const req = {
      taskId: form.taskId,
      operatorId: 'admin',
      overallResult: form.overallResult,
      remark: form.remark,
      reworkNode: form.reworkNode,
      items: form.items
    }
    // 1. 调用详细质检接口
    await request.post('/v1/qt/inspect/detail', req)
    // 2. 更新生产任务状态
    await request.post(`/v1/prod/tasks/${form.taskId}/quality`, {
      result: form.overallResult,
      operatorId: 'admin',
      remark: form.remark,
      reworkNode: form.reworkNode
    })
    ElMessage.success('质检已提交')
    detailInspectVisible.value = false
    fetchPendingTasks()
    fetchData()
  } catch (e) {}
}

onMounted(() => {
  fetchPendingTasks()
  fetchData()
})
</script>

<style scoped>
.inspect-item-row {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 8px;
}
.inspect-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.inspect-item-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
}
</style>
