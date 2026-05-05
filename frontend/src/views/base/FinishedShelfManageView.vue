<template>
  <div>
    <div class="page-header-title">成品货架管理：<span class="page-header-sub">区域、编码、容量等主数据维护</span></div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>货架列表</span>
          <el-button type="primary" data-testid="create-btn" @click="openDialog()">新增货架</el-button>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="区域编码">
          <el-input v-model="search.areaCode" placeholder="如 A" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="search.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="2" />
            <el-option label="维护中" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="search.keyword" placeholder="编码/名称/区域名" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" data-testid="search-btn" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border data-testid="data-table">
        <el-table-column prop="shelfCode" label="货架编码" min-width="120" />
        <el-table-column prop="shelfName" label="货架名称" min-width="120" />
        <el-table-column prop="areaCode" label="区域编码" width="100" />
        <el-table-column prop="areaName" label="区域名称" min-width="100" />
        <el-table-column prop="rowNum" label="排号" width="80" />
        <el-table-column prop="layerNum" label="层号" width="80" />
        <el-table-column prop="capacity" label="容量(袋)" width="100" />
        <el-table-column prop="currentCount" label="当前数量" width="100" />
        <el-table-column prop="shelfType" label="类型" width="110">
          <template #default="{ row }">
            <span>{{ shelfTypeLabel(row.shelfType) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无数据" />
      <el-pagination
        v-if="total > 0"
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑货架' : '新增货架'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="货架编码" prop="shelfCode">
          <el-input v-model="form.shelfCode" :disabled="!!form.id" placeholder="唯一编码，如 A-01-01" />
        </el-form-item>
        <el-form-item label="货架名称">
          <el-input v-model="form.shelfName" placeholder="可选" />
        </el-form-item>
        <el-form-item label="区域编码">
          <el-input v-model="form.areaCode" placeholder="如 A" />
        </el-form-item>
        <el-form-item label="区域名称">
          <el-input v-model="form.areaName" placeholder="如 A区" />
        </el-form-item>
        <el-form-item label="排号">
          <el-input-number v-model="form.rowNum" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="层号">
          <el-input-number v-model="form.layerNum" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="容量(袋)" prop="capacity">
          <el-input-number v-model="form.capacity" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.shelfType" placeholder="请选择" style="width: 100%">
            <el-option label="常温" value="NORMAL" />
            <el-option label="冷藏" value="COLD" />
            <el-option label="快递专区" value="EXPRESS" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="2">停用</el-radio>
            <el-radio :label="3">维护中</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button data-testid="dialog-cancel-btn" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" data-testid="dialog-save-btn" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getShelfPage, createShelfMaster, updateShelfMaster, deleteShelfMaster } from '@/api/shelf'

interface ShelfRow {
  id: number
  shelfCode: string
  shelfName?: string
  areaCode?: string
  areaName?: string
  rowNum?: number
  layerNum?: number
  capacity?: number
  currentCount?: number
  shelfType?: string
  status?: number
  updatedAt?: string
}

const list = ref<ShelfRow[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const page = ref(1)
const size = ref(20)
const total = ref(0)

const search = reactive<{ areaCode: string; status: number | ''; keyword: string }>({
  areaCode: '',
  status: '',
  keyword: ''
})

const form = ref<Partial<ShelfRow>>({
  shelfType: 'NORMAL',
  status: 1
})

const rules = {
  shelfCode: [{ required: true, message: '请输入货架编码', trigger: 'blur' }],
  capacity: [{ required: true, message: '请填写容量', trigger: 'change' }]
}

function statusText(s?: number) {
  if (s === 1) return '启用'
  if (s === 2) return '停用'
  if (s === 3) return '维护中'
  return s == null ? '-' : String(s)
}

function statusTagType(s?: number) {
  if (s === 1) return 'success'
  if (s === 2) return 'info'
  if (s === 3) return 'warning'
  return ''
}

function shelfTypeLabel(t?: string) {
  if (t === 'NORMAL') return '常温'
  if (t === 'COLD') return '冷藏'
  if (t === 'EXPRESS') return '快递专区'
  return t || '-'
}

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { page: page.value, size: size.value }
    if (search.areaCode) params.areaCode = search.areaCode.trim()
    if (search.status !== '') params.status = search.status
    if (search.keyword) params.keyword = search.keyword.trim()
    const res: any = await getShelfPage(params)
    const data = res.data
    list.value = data?.records || []
    total.value = data?.total ?? 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchData()
}

function resetSearch() {
  search.areaCode = ''
  search.status = ''
  search.keyword = ''
  handleSearch()
}

function openDialog(row?: ShelfRow) {
  if (row) {
    form.value = {
      id: row.id,
      shelfCode: row.shelfCode,
      shelfName: row.shelfName,
      areaCode: row.areaCode,
      areaName: row.areaName,
      rowNum: row.rowNum,
      layerNum: row.layerNum,
      capacity: row.capacity,
      shelfType: row.shelfType || 'NORMAL',
      status: row.status ?? 1
    }
  } else {
    form.value = { shelfType: 'NORMAL', status: 1, capacity: 20 }
  }
  dialogVisible.value = true
}

async function handleSave() {
  const ok = await formRef.value?.validate().catch(() => false)
  if (!ok) return
  try {
    const payload: Record<string, unknown> = {
      shelfCode: form.value.shelfCode,
      shelfName: form.value.shelfName,
      areaCode: form.value.areaCode,
      areaName: form.value.areaName,
      rowNum: form.value.rowNum,
      layerNum: form.value.layerNum,
      capacity: form.value.capacity,
      shelfType: form.value.shelfType,
      status: form.value.status
    }
    if (form.value.id) {
      await updateShelfMaster(form.value.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createShelfMaster(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '保存失败'
    ElMessage.error(msg)
  }
}

async function handleDelete(row: ShelfRow) {
  try {
    await ElMessageBox.confirm(`确认删除货架「${row.shelfCode}」？（需当前数量为 0）`, '提示', { type: 'warning' })
    await deleteShelfMaster(row.id)
    ElMessage.success('已删除')
    fetchData()
  } catch (e: any) {
    if (e !== 'cancel') {
      const msg = e?.response?.data?.message || e?.message || '删除失败'
      ElMessage.error(msg)
    }
  }
}

onMounted(() => fetchData())
</script>
