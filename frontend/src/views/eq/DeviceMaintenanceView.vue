<template>
  <div class="page-container">
    <el-card class="mt-4" shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>维保记录</span>
          <el-button type="primary" @click="openDialog()">新增记录</el-button>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="设备">
          <el-input v-model="search.deviceId" placeholder="设备ID" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="维保类型">
          <el-select v-model="search.maintenanceType" placeholder="全部" clearable style="width: 140px">
            <el-option label="日常保养" value="DAILY" />
            <el-option label="定期检修" value="PERIODIC" />
            <el-option label="故障维修" value="REPAIR" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="search.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="待执行" :value="0" />
            <el-option label="已完成" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="deviceName" label="设备名称" width="120" />
        <el-table-column prop="maintenanceType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.maintenanceType === 'DAILY'">日常保养</el-tag>
            <el-tag v-else-if="row.maintenanceType === 'PERIODIC'" type="warning">定期检修</el-tag>
            <el-tag v-else type="danger">故障维修</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="维保内容" min-width="200" />
        <el-table-column prop="maintenanceDate" label="维保日期" width="120" />
        <el-table-column prop="nextDate" label="下次保养" width="120" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success">已完成</el-tag>
            <el-tag v-else>待执行</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑维保记录' : '新增维保记录'" width="500px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="设备ID">
          <el-input v-model="form.deviceId" placeholder="设备ID" />
        </el-form-item>
        <el-form-item label="维保类型">
          <el-select v-model="form.maintenanceType" placeholder="选择类型" style="width: 100%">
            <el-option label="日常保养" value="DAILY" />
            <el-option label="定期检修" value="PERIODIC" />
            <el-option label="故障维修" value="REPAIR" />
          </el-select>
        </el-form-item>
        <el-form-item label="维保内容">
          <el-input v-model="form.content" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="维保日期">
          <el-date-picker v-model="form.maintenanceDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="下次保养">
          <el-date-picker v-model="form.nextDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="0">待执行</el-radio>
            <el-radio :label="1">已完成</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveData">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const tableData = ref<any[]>([])
const dialogVisible = ref(false)
const search = reactive({ deviceId: '', maintenanceType: '', status: null as number | null })
const pagination = reactive({ page: 1, size: 20, total: 0 })
const form = reactive<any>({ id: null, deviceId: '', maintenanceType: '', content: '', maintenanceDate: '', nextDate: '', status: 0 })
const formRef = ref<any>(null)
const formRules = {
  deviceId: [{ required: true, message: '设备ID不能为空', trigger: 'blur' }],
  maintenanceType: [{ required: true, message: '维保类型不能为空', trigger: 'change' }],
  content: [{ required: true, message: '维保内容不能为空', trigger: 'blur' }],
  maintenanceDate: [{ required: true, message: '维保日期不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }],
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await request.get('/v1/eq/device-maintenances', {
      params: { ...search, page: pagination.page, size: pagination.size }
    })
    const pageData = res.data || {}
    tableData.value = pageData.records || []
    pagination.total = pageData.total || 0
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  search.deviceId = ''
  search.maintenanceType = ''
  search.status = null
  pagination.page = 1
  loadData()
}

function openDialog(row?: any) {
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, { id: null, deviceId: '', maintenanceType: '', content: '', maintenanceDate: '', nextDate: '', status: 0 })
  }
  dialogVisible.value = true
}

async function saveData() {
  if (!formRef.value) return
  await formRef.value.validate()
  try {
    if (form.id) {
      await request.put(`/v1/eq/device-maintenances/${form.id}`, form)
    } else {
      await request.post('/v1/eq/device-maintenances', form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
    await request.delete(`/v1/eq/device-maintenances/${id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // cancel
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-container { padding: 16px; }
.mt-4 { margin-top: 16px; }
.pagination-wrapper { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
