<template>
  <div>
    <div class="page-header-title">人员管理：<span class="page-header-sub">员工信息、角色分配、条码管理</span></div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>用户管理</span>
          <el-button type="primary" v-if="userStore.hasPermission('sys:user:create')" @click="openDialog()">新增用户</el-button>
        </div>
      </template>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="search.keyword" placeholder="用户名/手机号" clearable />
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
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" v-if="userStore.hasPermission('sys:user:update')" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="warning" v-if="userStore.hasPermission('sys:role:assign')" @click="openRoleDialog(row)">分配角色</el-button>
            <el-button size="small" type="danger" v-if="userStore.hasPermission('sys:user:delete')" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="密码" :required="!form.id">
          <el-input v-model="form.password" type="password" placeholder="不填则保持不变" />
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

    <!-- 分配角色 -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="400px">
      <el-form label-width="60px">
        <el-form-item label="用户">
          <span>{{ currentUser?.username }}</span>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="selectedRoleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in allRoles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
const dialogVisible = ref(false)
const roleDialogVisible = ref(false)
const currentUser = ref<User | null>(null)
const selectedRoleIds = ref<number[]>([])
const allRoles = ref<Role[]>([])

const search = ref({ keyword: '' })
const pagination = ref({ page: 1, size: 10, total: 0 })
const form = ref<Partial<User & { password?: string }>>({})

async function fetchData() {
  loading.value = true
  try {
    const res: any = await request.get('/v1/sys/users', {
      params: { page: pagination.value.page, size: pagination.value.size, keyword: search.value.keyword }
    })
    list.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function openDialog(row?: User) {
  form.value = row ? { ...row } : { status: 1 }
  dialogVisible.value = true
}

async function handleSave() {
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
  } catch (e) {
    // error handled by interceptor
  }
}

async function handleDelete(row: User) {
  try {
    await ElMessageBox.confirm('确认删除该用户？', '提示', { type: 'warning' })
    await request.delete(`/v1/sys/users/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    // cancelled
  }
}

async function openRoleDialog(row: User) {
  currentUser.value = row
  roleDialogVisible.value = true
  selectedRoleIds.value = []
  try {
    const res: any = await request.get(`/v1/rbac/users/${row.id}/roles`)
    selectedRoleIds.value = (res.data || []).map((r: Role) => r.id)
  } catch (e) {}
}

async function handleAssignRoles() {
  if (!currentUser.value) return
  try {
    await request.post(`/v1/rbac/users/${currentUser.value.id}/roles`, selectedRoleIds.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
  } catch (e) {}
}

async function fetchRoles() {
  try {
    const res: any = await request.get('/v1/rbac/roles')
    allRoles.value = res.data || []
  } catch (e) {}
}

onMounted(() => {
  fetchData()
  fetchRoles()
})
</script>
