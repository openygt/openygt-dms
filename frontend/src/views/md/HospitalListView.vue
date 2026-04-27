<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>医院管理</span>
          <el-button type="primary" @click="openDialog()">新增医院</el-button>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="医院名称">
          <el-input v-model="search.name" placeholder="医院名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="search.name = ''; fetchData()">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="hospitalName" label="医院名称" />
        <el-table-column prop="hospitalCode" label="医院编码" />
        <el-table-column prop="contactName" label="联系人" />
        <el-table-column prop="contactPhone" label="联系电话" />
        <el-table-column prop="address" label="地址" />
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
      <el-empty v-if="!loading && list.length === 0" description="暂无医院" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑医院' : '新增医院'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="医院名称" required>
          <el-input v-model="form.hospitalName" />
        </el-form-item>
        <el-form-item label="医院编码" required>
          <el-input v-model="form.hospitalCode" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

interface Hospital {
  id: number
  hospitalName: string
  hospitalCode: string
  contactName: string
  contactPhone: string
  address: string
  status: number
}

const list = ref<Hospital[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const search = ref({ name: '' })
const form = ref<Partial<Hospital>>({ status: 1 })

async function fetchData() {
  loading.value = true
  try {
    const params: any = {}
    if (search.value.name) params.keyword = search.value.name
    const res: any = await request.get('/v1/md/hospitals', { params })
    list.value = res.data?.records || []
  } finally {
    loading.value = false
  }
}

function openDialog(row?: Hospital) {
  form.value = row ? { ...row } : { status: 1 }
  dialogVisible.value = true
}

async function handleSave() {
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
  } catch (e) {}
}

async function handleDelete(row: Hospital) {
  try {
    await ElMessageBox.confirm('确认删除该医院？', '提示', { type: 'warning' })
    await request.delete(`/v1/md/hospitals/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {}
}

onMounted(fetchData)
</script>
