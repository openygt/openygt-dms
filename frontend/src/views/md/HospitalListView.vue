<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>医院管理</span>
          <el-button type="primary" v-if="userStore.hasPermission('md:hospital:create')" @click="openDialog()">新增医院</el-button>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="医院名称">
          <el-input v-model="search.keyword" placeholder="医院名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="search.keyword = ''; fetchData()">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="医院名称" />
        <el-table-column prop="code" label="医院编码" />
        <el-table-column prop="contactPerson" label="联系人" />
        <el-table-column prop="phone" label="联系电话" />

        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" v-if="userStore.hasPermission('md:hospital:update')" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" v-if="userStore.hasPermission('md:hospital:delete')" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无医院" />
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑医院' : '新增医院'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="医院名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="医院编码" prop="code">
          <el-input v-model="form.code" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactPerson" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/api/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

interface Hospital {
  id: number
  name: string
  code: string
  contactPerson: string
  phone: string
  address: string
  status: number
}

const list = ref<Hospital[]>([])
const loading = ref(false)
const saveLoading = ref(false)
const dialogVisible = ref(false)
const search = ref({ keyword: '' })
const form = ref<Partial<Hospital>>({ status: 1 })
const formRef = ref<FormInstance>()
const page = ref(1)
const size = ref(20)
const total = ref(0)

const rules: FormRules = {
  name: [{ required: true, message: '请输入医院名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入医院编码', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (search.value.keyword) params.keyword = search.value.keyword
    const res: any = await request.get('/v1/md/hospitals', { params })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '查询失败')
  } finally {
    loading.value = false
  }
}

function openDialog(row?: Hospital) {
  form.value = row ? { ...row } : { status: 1 }
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
      await request.put(`/v1/md/hospitals/${form.value.id}`, form.value)
      ElMessage.success('更新成功')
    } else {
      await request.post('/v1/md/hospitals', form.value)
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

async function handleDelete(row: Hospital) {
  try {
    await ElMessageBox.confirm('确认删除该医院？', '提示', { type: 'warning' })
    await request.delete(`/v1/md/hospitals/${row.id}`)
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
