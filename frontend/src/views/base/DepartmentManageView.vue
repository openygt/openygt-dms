<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>科室管理</span>
          <el-button type="primary" v-if="userStore.hasPermission('md:dept:create')" data-testid="create-btn" @click="openDialog()">新增科室</el-button>
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
        <el-table-column prop="sourceId" label="源ID" width="180" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="leader" label="负责人" width="120" />
        <el-table-column prop="chairman" label="主管" width="120" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" v-if="userStore.hasPermission('md:dept:update')" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" v-if="userStore.hasPermission('md:dept:delete')" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑科室' : '新增科室'" width="560px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="源ID" prop="sourceId">
          <el-input v-model="form.sourceId" />
        </el-form-item>
        <el-form-item label="科室名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="父节点ID">
          <el-input v-model="form.parentId" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.leader" />
        </el-form-item>
        <el-form-item label="主管">
          <el-input v-model="form.chairman" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input v-model.number="form.sort" type="number" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button data-testid="dialog-cancel-btn" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" data-testid="dialog-save-btn" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getDepartmentList,
  createDepartment,
  updateDepartment,
  deleteDepartment
} from '@/api/baseData'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

interface Department {
  id: number
  sourceId: string
  name: string
  parentId: string
  leader: string
  chairman: string
  sort: number
}

const list = ref<Department[]>([])
const loading = ref(false)
const saveLoading = ref(false)
const dialogVisible = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const formRef = ref<FormInstance>()

const search = reactive({ keyword: '' })
const form = ref<Partial<Department>>({})
const rules: FormRules = {
  sourceId: [{ required: true, message: '请输入源ID', trigger: 'blur' }],
  name: [{ required: true, message: '请输入科室名称', trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (search.keyword) params.keyword = search.keyword
    const res: any = await getDepartmentList(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '查询失败')
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

function openDialog(row?: Department) {
  form.value = row ? { ...row } : {}
  dialogVisible.value = true
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

async function handleSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saveLoading.value = true
  try {
    if (form.value.id) {
      await updateDepartment(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createDepartment(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  } finally {
    saveLoading.value = false
  }
}

async function handleDelete(row: Department) {
  try {
    await ElMessageBox.confirm('确认删除该科室？', '提示', { type: 'warning' })
    await deleteDepartment(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || e?.message || '删除失败')
    }
  }
}

onMounted(() => {
  fetchData()
})
</script>
