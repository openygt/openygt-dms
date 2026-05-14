<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          
          <el-button type="primary" v-if="userStore.hasPermission('sys:user:create')" @click="openDialog()">新增用户</el-button>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="search.keyword" placeholder="用户名/姓名/手机号" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="search.keyword = ''; fetchData()">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="isActiveStatus(row.status) ? 'success' : 'danger'">{{ isActiveStatus(row.status) ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" v-if="userStore.hasPermission('sys:user:update')" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="warning" v-if="userStore.hasPermission('sys:role:assign')" @click="openRoleDialog(row)">分配角色</el-button>
            <el-button size="small" type="danger" v-if="userStore.hasPermission('sys:user:delete')" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无数据" />
      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" :placeholder="form.id ? '不填则保持不变' : ''" />
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

    <!-- 分配角色 -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="400px">
      <el-form label-width="60px">
        <el-form-item label="用户">
          <span>{{ currentUser?.username }}</span>
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="selectedRoleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in allRoles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="handleAssignRoles">保存</el-button>
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

interface User {
  id: number
  username: string
  realName: string
  phone: string
  status: number
  createdAt: string
}

interface Role {
  id: number
  roleName: string
}

const list = ref<User[]>([])
const loading = ref(false)
const saveLoading = ref(false)
const assignLoading = ref(false)
const dialogVisible = ref(false)
const roleDialogVisible = ref(false)
const currentUser = ref<User | null>(null)
const selectedRoleIds = ref<number[]>([])
const allRoles = ref<Role[]>([])
const formRef = ref<FormInstance>()

const search = ref({ keyword: '' })
const pagination = ref({ page: 1, size: 10, total: 0 })
const form = ref<Partial<User & { password?: string }>>({})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{
    validator: (_rule: any, value: any, callback: any) => {
      if (!form.value.id && (!value || value.trim() === '')) {
        callback(new Error('请输入密码'))
      } else {
        callback()
      }
    },
    trigger: 'blur'
  }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await request.get('/v1/sys/users', {
      params: { page: pagination.value.page, size: pagination.value.size, keyword: search.value.keyword }
    })
    list.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '查询失败')
  } finally {
    loading.value = false
  }
}

function isActiveStatus(status: any): boolean {
  return status === 1 || status === '1' || status === 'ACTIVE'
}

function openDialog(row?: User) {
  if (row) {
    const statusNum = isActiveStatus(row.status) ? 1 : 0
    // 编辑时剔除 password，避免把后端 bcrypt 哈希带到表单再次加密
    const { password: _, ...rest } = row as any
    form.value = { ...rest, status: statusNum }
  } else {
    form.value = { status: 1 }
  }
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
      const payload: any = { ...form.value }
      if (!payload.password) delete payload.password
      await request.put(`/v1/sys/users/${form.value.id}`, payload)
      ElMessage.success('更新成功')
    } else {
      await request.post('/v1/sys/users', form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saveLoading.value = false
  }
}

async function handleDelete(row: User) {
  try {
    await ElMessageBox.confirm('确认删除该用户？', '提示', { type: 'warning' })
    await request.delete(`/v1/sys/users/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '删除失败')
    }
  }
}

async function openRoleDialog(row: User) {
  currentUser.value = row
  roleDialogVisible.value = true
  selectedRoleIds.value = []
  try {
    const res: any = await request.get(`/v1/rbac/users/${row.id}/roles`)
    selectedRoleIds.value = (res.data || []).map((r: Role) => r.id)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '获取角色失败')
  }
}

async function handleAssignRoles() {
  if (!currentUser.value) return
  if (selectedRoleIds.value.length === 0) {
    ElMessage.warning('请至少选择一个角色')
    return
  }
  assignLoading.value = true
  try {
    await request.post(`/v1/rbac/users/${currentUser.value.id}/roles`, selectedRoleIds.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '分配失败')
  } finally {
    assignLoading.value = false
  }
}

async function fetchRoles() {
  try {
    const res: any = await request.get('/v1/rbac/roles')
    allRoles.value = res.data || []
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '获取角色列表失败')
  }
}

function formatDateTime(dt: string) {
  if (!dt) return '-'
  const d = new Date(dt)
  if (isNaN(d.getTime())) return dt
  return d.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

onMounted(() => {
  fetchData()
  fetchRoles()
})
</script>
