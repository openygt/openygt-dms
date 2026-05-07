<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>药材目录</span>
          <el-button type="primary" v-if="userStore.hasPermission('md:herb:create')" data-testid="create-btn" @click="openDialog()">新增药材</el-button>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="search.keyword" placeholder="编码/名称/别名" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" data-testid="search-btn" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border data-testid="data-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="code" label="编码" width="120" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="pinyin" label="拼音" width="120" />
        <el-table-column prop="unitName" label="单位" width="80" />
        <el-table-column prop="drugLevel" label="毒性" width="80" />
        <el-table-column prop="isEnabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled === 1 ? 'success' : 'danger'">{{ row.isEnabled === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" v-if="userStore.hasPermission('md:herb:update')" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" v-if="userStore.hasPermission('md:herb:delete')" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑药材' : '新增药材'" width="560px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="药材编码" prop="code">
          <el-input v-model="form.code" />
        </el-form-item>
        <el-form-item label="药材名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="拼音">
          <el-input v-model="form.pinyin" />
        </el-form-item>
        <el-form-item label="单位" prop="unitName">
          <el-input v-model="form.unitName" />
        </el-form-item>
        <el-form-item label="毒性分类">
          <el-input v-model="form.drugLevel" />
        </el-form-item>
        <el-form-item label="功效分类">
          <el-input v-model="form.efficacyCategory" />
        </el-form-item>
        <el-form-item label="药用部位">
          <el-input v-model="form.medicinalPart" />
        </el-form-item>
        <el-form-item label="炮制方法">
          <el-input v-model="form.processingMethod" />
        </el-form-item>
        <el-form-item label="状态" prop="isEnabled">
          <el-radio-group v-model="form.isEnabled">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
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
  getMedicineList,
  createMedicine,
  updateMedicine,
  deleteMedicine
} from '@/api/baseData'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

interface Medicine {
  id: number
  code: string
  name: string
  pinyin: string
  unitName: string
  drugLevel: string
  efficacyCategory: string
  medicinalPart: string
  processingMethod: string
  isEnabled: number
}

const list = ref<Medicine[]>([])
const loading = ref(false)
const saveLoading = ref(false)
const dialogVisible = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const formRef = ref<FormInstance>()

const search = reactive({ keyword: '' })
const form = ref<Partial<Medicine>>({ isEnabled: 1 })

const rules: FormRules = {
  code: [{ required: true, message: '请输入药材编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入药材名称', trigger: 'blur' }],
  unitName: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  isEnabled: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (search.keyword) params.keyword = search.keyword
    const res: any = await getMedicineList(params)
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

function openDialog(row?: Medicine) {
  form.value = row ? { ...row } : { isEnabled: 1 }
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
      await updateMedicine(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createMedicine(form.value)
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

async function handleDelete(row: Medicine) {
  try {
    await ElMessageBox.confirm('确认删除该药材？', '提示', { type: 'warning' })
    await deleteMedicine(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || e?.message || '删除失败')
    }
  }
}

onMounted(fetchData)
</script>
