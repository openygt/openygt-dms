<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>煎煮方案</span>
          <el-button type="primary" @click="openDialog()">新增方案</el-button>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="方案名称">
          <el-input v-model="search.name" placeholder="方案名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="search.name = ''; fetchData()">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="schemeName" label="方案名称" />
        <el-table-column prop="schemeCode" label="方案编码" />
        <el-table-column prop="soakTime" label="浸泡时间(分)" width="120" />
        <el-table-column prop="decoctTime" label="煎煮时间(分)" width="120" />
        <el-table-column prop="tempRange" label="温度范围" />
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
      <el-empty v-if="!loading && list.length === 0" description="暂无方案" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑方案' : '新增方案'" width="500px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="方案名称" required>
          <el-input v-model="form.schemeName" />
        </el-form-item>
        <el-form-item label="方案编码" required>
          <el-input v-model="form.schemeCode" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="浸泡时间(分)">
          <el-input-number v-model="form.soakTime" :min="0" />
        </el-form-item>
        <el-form-item label="煎煮时间(分)">
          <el-input-number v-model="form.decoctTime" :min="0" />
        </el-form-item>
        <el-form-item label="温度范围">
          <el-input v-model="form.tempRange" placeholder="如 100-105°C" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" rows="2" />
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

interface Scheme {
  id: number
  schemeName: string
  schemeCode: string
  soakTime: number
  decoctTime: number
  tempRange: string
  status: number
  remark: string
}

const list = ref<Scheme[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const search = ref({ name: '' })
const form = ref<Partial<Scheme>>({ status: 1 })

async function fetchData() {
  loading.value = true
  try {
    const params: any = {}
    if (search.value.name) params.keyword = search.value.name
    const res: any = await request.get('/v1/md/schemes', { params })
    list.value = res.data?.records || []
  } finally {
    loading.value = false
  }
}

function openDialog(row?: Scheme) {
  form.value = row ? { ...row } : { status: 1 }
  dialogVisible.value = true
}

async function handleSave() {
  try {
    if (form.value.id) {
      await request.put(`/v1/md/schemes/${form.value.id}`, form.value)
      ElMessage.success('更新成功')
    } else {
      await request.post('/v1/md/schemes', form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {}
}

async function handleDelete(row: Scheme) {
  try {
    await ElMessageBox.confirm('确认删除该方案？', '提示', { type: 'warning' })
    await request.delete(`/v1/md/schemes/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {}
}

onMounted(fetchData)
</script>
