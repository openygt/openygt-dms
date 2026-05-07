<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <el-button type="primary" data-testid="create-btn" @click="openDialog()">新增规格</el-button>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="search.keyword" placeholder="编码/名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" data-testid="search-btn" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border data-testid="data-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="specCode" label="编码" width="120" />
        <el-table-column prop="specName" label="名称" />
        <el-table-column prop="volumeMl" label="容量(ml)" width="100" />
        <el-table-column prop="bagType" label="袋型" width="100" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑规格' : '新增规格'" width="560px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="规格编码" required>
          <el-input v-model="form.specCode" />
        </el-form-item>
        <el-form-item label="规格名称" required>
          <el-input v-model="form.specName" />
        </el-form-item>
        <el-form-item label="容量(ml)">
          <el-input v-model.number="form.volumeMl" type="number" />
        </el-form-item>
        <el-form-item label="袋型">
          <el-select v-model="form.bagType" placeholder="请选择袋型" style="width: 100%">
            <el-option label="普通" value="普通" />
            <el-option label="真空" value="真空" />
            <el-option label="铝箔" value="铝箔" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input v-model.number="form.sortOrder" type="number" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
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
import {
  getPackageSpecList,
  createPackageSpec,
  updatePackageSpec,
  deletePackageSpec
} from '@/api/baseData'

interface PackageSpec {
  id: number
  specCode: string
  specName: string
  volumeMl: number
  bagType: string
  description: string
  sortOrder: number
  status: number
}

const list = ref<PackageSpec[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

const search = reactive({ keyword: '' })
const form = ref<Partial<PackageSpec>>({ status: 1 })
const formRef = ref<any>(null)
const formRules = {
  specCode: [{ required: true, message: '规格编码不能为空', trigger: 'blur' }],
  specName: [{ required: true, message: '规格名称不能为空', trigger: 'blur' }],
  volumeMl: [{ required: true, message: '容量不能为空', trigger: 'change' }],
  bagType: [{ required: true, message: '袋型不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }],
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (search.keyword) params.keyword = search.keyword
    const res: any = await getPackageSpecList(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchData()
}

function resetSearch() {
  search.keyword = ''
  page.value = 1
  fetchData()
}

function openDialog(row?: PackageSpec) {
  form.value = row ? { ...row } : { status: 1 }
  dialogVisible.value = true
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate()
  try {
    if (form.value.id) {
      await updatePackageSpec(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createPackageSpec(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {}
}

async function handleDelete(row: PackageSpec) {
  try {
    await ElMessageBox.confirm('确认删除该规格？', '提示', { type: 'warning' })
    await deletePackageSpec(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {}
}

onMounted(fetchData)
</script>
