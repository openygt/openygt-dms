<template>
  <div>
    <div class="page-header-title">接口中心：<span class="page-header-sub">第三方系统对接、接口文档</span></div>
    <el-card>
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <span>接口中心</span>
          <el-button type="primary" @click="openDialog()">新增接口</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="query" style="margin-bottom: 16px">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="编码/名称" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="接口类型">
          <el-select v-model="query.interfaceType" placeholder="全部" clearable style="width: 140px">
            <el-option label="HIS" value="HIS" />
            <el-option label="设备" value="DEVICE" />
            <el-option label="第三方" value="THIRD_PARTY" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="interfaceCode" label="接口编码" width="120" />
        <el-table-column prop="interfaceName" label="接口名称" width="150" />
        <el-table-column prop="interfaceType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.interfaceType)">{{ typeText(row.interfaceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="protocol" label="协议" width="100" />
        <el-table-column prop="baseUrl" label="基础地址" show-overflow-tooltip />
        <el-table-column prop="authType" label="认证方式" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
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

    <!-- 接口日志 -->
    <el-card style="margin-top: 16px">
      <template #header>
        <span>接口调用日志</span>
      </template>
      <el-form :inline="true" :model="logQuery" style="margin-bottom: 16px">
        <el-form-item label="结果">
          <el-select v-model="logQuery.result" placeholder="全部" clearable style="width: 120px">
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAIL" />
            <el-option label="超时" value="TIMEOUT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchLogs">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="logList" v-loading="logLoading" border size="small">
        <el-table-column prop="interfaceCode" label="接口编码" width="120" />
        <el-table-column prop="direction" label="方向" width="80" />
        <el-table-column prop="method" label="方法" width="80" />
        <el-table-column prop="url" label="URL" show-overflow-tooltip />
        <el-table-column prop="statusCode" label="状态码" width="80" />
        <el-table-column prop="result" label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.result === 'SUCCESS' ? 'success' : 'danger'" size="small">{{ row.result }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时(ms)" width="90" />
        <el-table-column prop="createdAt" label="时间" width="160" />
      </el-table>
      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="logPagination.page"
        v-model:page-size="logPagination.size"
        :total="logPagination.total"
        layout="total, prev, pager, next"
        @current-change="fetchLogs"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="接口编码" required>
          <el-input v-model="form.interfaceCode" placeholder="如 HIS_001" />
        </el-form-item>
        <el-form-item label="接口名称" required>
          <el-input v-model="form.interfaceName" placeholder="如 HIS处方接口" />
        </el-form-item>
        <el-form-item label="接口类型" required>
          <el-select v-model="form.interfaceType" placeholder="请选择" style="width: 100%">
            <el-option label="HIS" value="HIS" />
            <el-option label="设备" value="DEVICE" />
            <el-option label="第三方" value="THIRD_PARTY" />
          </el-select>
        </el-form-item>
        <el-form-item label="协议">
          <el-select v-model="form.protocol" placeholder="请选择" style="width: 100%">
            <el-option label="REST" value="REST" />
            <el-option label="WebService" value="WEBSERVICE" />
            <el-option label="HL7" value="HL7" />
            <el-option label="MQTT" value="MQTT" />
            <el-option label="Modbus" value="MODBUS" />
            <el-option label="TCP" value="TCP" />
          </el-select>
        </el-form-item>
        <el-form-item label="基础地址">
          <el-input v-model="form.baseUrl" placeholder="http://example.com/api" />
        </el-form-item>
        <el-form-item label="认证方式">
          <el-select v-model="form.authType" placeholder="请选择" style="width: 100%">
            <el-option label="无" value="NONE" />
            <el-option label="Basic" value="BASIC" />
            <el-option label="Token" value="TOKEN" />
            <el-option label="OAuth2" value="OAUTH2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getInterfaceConfigs,
  createInterfaceConfig,
  updateInterfaceConfig,
  deleteInterfaceConfig,
  getInterfaceLogs
} from '@/api/interfaceCenter'

const list = ref<any[]>([])
const loading = ref(false)
const logList = ref<any[]>([])
const logLoading = ref(false)

const query = ref({ keyword: '', interfaceType: '' })
const pagination = ref({ page: 1, size: 20, total: 0 })

const logQuery = ref({ result: '' })
const logPagination = ref({ page: 1, size: 10, total: 0 })

const dialogVisible = ref(false)
const dialogTitle = ref('新增接口')
const form = ref<any>({ status: 1 })
const isEdit = ref(false)

function typeTag(type?: string) {
  if (type === 'HIS') return 'primary'
  if (type === 'DEVICE') return 'warning'
  if (type === 'THIRD_PARTY') return 'success'
  return ''
}

function typeText(type?: string) {
  if (type === 'HIS') return 'HIS'
  if (type === 'DEVICE') return '设备'
  if (type === 'THIRD_PARTY') return '第三方'
  return type || '-'
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = { page: pagination.value.page, size: pagination.value.size }
    if (query.value.keyword) params.keyword = query.value.keyword
    if (query.value.interfaceType) params.interfaceType = query.value.interfaceType
    const res: any = await getInterfaceConfigs(params)
    list.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function fetchLogs() {
  logLoading.value = true
  try {
    const params: any = { page: logPagination.value.page, size: logPagination.value.size }
    if (logQuery.value.result) params.result = logQuery.value.result
    const res: any = await getInterfaceLogs(params)
    logList.value = res.data?.records || []
    logPagination.value.total = res.data?.total || 0
  } finally {
    logLoading.value = false
  }
}

function handleSearch() {
  pagination.value.page = 1
  fetchData()
}

function handleReset() {
  query.value = { keyword: '', interfaceType: '' }
  pagination.value.page = 1
  fetchData()
}

function openDialog(row?: any) {
  if (row) {
    isEdit.value = true
    dialogTitle.value = '编辑接口'
    form.value = { ...row }
  } else {
    isEdit.value = false
    dialogTitle.value = '新增接口'
    form.value = { status: 1 }
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.interfaceCode || !form.value.interfaceName || !form.value.interfaceType) {
    ElMessage.warning('请填写必填项')
    return
  }
  try {
    if (isEdit.value) {
      await updateInterfaceConfig(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createInterfaceConfig(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {}
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确认删除该接口配置？', '提示', { type: 'warning' })
    await deleteInterfaceConfig(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {}
}

onMounted(() => {
  fetchData()
  fetchLogs()
})
</script>
