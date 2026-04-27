<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>质检记录</span>
          <el-button type="primary" @click="openDialog()">新增质检</el-button>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="任务号">
          <el-input v-model="search.taskId" placeholder="任务ID" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="search.taskId = ''; fetchData()">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="taskId" label="任务号" width="100" />
        <el-table-column prop="result" label="结果" width="120">
          <template #default="{ row }">
            <el-tag :type="row.result === 'PASS' ? 'success' : row.result === 'FAIL' ? 'danger' : 'warning'">
              {{ resultText(row.result) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="inspector" label="质检员" />
        <el-table-column prop="remark" label="备注" />
        <el-table-column prop="createdAt" label="质检时间" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无质检记录" />
    </el-card>

    <!-- 新增质检 -->
    <el-dialog v-model="dialogVisible" title="新增质检" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="任务号" required>
          <el-input v-model="form.taskId" />
        </el-form-item>
        <el-form-item label="质检结果" required>
          <el-radio-group v-model="form.result">
            <el-radio label="PASS">合格</el-radio>
            <el-radio label="FAIL">不合格</el-radio>
            <el-radio label="REWORK">返工</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="质检详情" width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="任务号">{{ detail?.taskId }}</el-descriptions-item>
        <el-descriptions-item label="结果">
          <el-tag :type="detail?.result === 'PASS' ? 'success' : detail?.result === 'FAIL' ? 'danger' : 'warning'">
            {{ resultText(detail?.result) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="质检员">{{ detail?.inspector }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail?.remark }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ detail?.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

interface QcRecord {
  id: number
  taskId: number
  result: string
  inspector: string
  remark: string
  createdAt: string
}

const list = ref<QcRecord[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const detail = ref<QcRecord | null>(null)
const search = ref({ taskId: '' })
const form = ref<Partial<QcRecord>>({ result: 'PASS' })

function resultText(result?: string) {
  const map: Record<string, string> = { 'PASS': '合格', 'FAIL': '不合格', 'REWORK': '返工' }
  return map[result || ''] || result
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = {}
    if (search.value.taskId) params.taskId = search.value.taskId
    const res: any = await request.get('/v1/qt/inspection/' + (search.value.taskId || '0'))
    list.value = res.data ? [res.data] : []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function openDialog() {
  form.value = { result: 'PASS' }
  dialogVisible.value = true
}

async function handleSave() {
  try {
    await request.post('/v1/qt/inspect', form.value)
    ElMessage.success('质检记录已保存')
    dialogVisible.value = false
    fetchData()
  } catch (e) {}
}

function viewDetail(row: QcRecord) {
  detail.value = row
  detailVisible.value = true
}

onMounted(fetchData)
</script>
