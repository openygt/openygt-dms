<template>
  <div>
    <div class="page-header-title">参数配置：<span class="page-header-sub">系统参数、业务规则、开关配置</span></div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>系统配置</span>
          <el-button type="primary" @click="openDialog()">新增配置</el-button>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="配置键">
          <el-input v-model="search.configKey" placeholder="配置键" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="search.configKey = ''; fetchData()">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="configKey" label="配置键" />
        <el-table-column prop="configValue" label="配置值" />
        <el-table-column prop="description" label="说明" />
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无配置" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑配置' : '新增配置'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="配置键" required>
          <el-input v-model="form.configKey" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="配置值" required>
          <el-input v-model="form.configValue" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" rows="2" />
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

interface SysConfig {
  id: number
  configKey: string
  configValue: string
  description: string
  createdAt: string
}

const list = ref<SysConfig[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const search = ref({ configKey: '' })
const form = ref<Partial<SysConfig>>({})

async function fetchData() {
  loading.value = true
  try {
    const params: any = {}
    if (search.value.configKey) params.keyword = search.value.configKey
    const res: any = await request.get('/v1/sys/configs', { params })
    list.value = res.data?.records || []
  } finally {
    loading.value = false
  }
}

function openDialog(row?: SysConfig) {
  form.value = row ? { ...row } : {}
  dialogVisible.value = true
}

async function handleSave() {
  try {
    if (form.value.id) {
      await request.put(`/v1/sys/configs/${form.value.id}`, form.value)
      ElMessage.success('更新成功')
    } else {
      await request.post('/v1/sys/configs', form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {}
}

async function handleDelete(row: SysConfig) {
  try {
    await ElMessageBox.confirm('确认删除该配置？', '提示', { type: 'warning' })
    await request.delete(`/v1/sys/configs/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {}
}

onMounted(fetchData)
</script>
