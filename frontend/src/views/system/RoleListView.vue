<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <el-form :inline="true" @submit.prevent style="margin-bottom: 0">
            <el-form-item label="关键词" style="margin-bottom: 0">
              <el-input v-model="search.keyword" placeholder="角色名称/编码" clearable />
            </el-form-item>
            <el-form-item style="margin-bottom: 0">
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="search.keyword = ''; handleSearch()">重置</el-button>
            </el-form-item>
          </el-form>
          <el-button type="primary" v-if="userStore.hasPermission('sys:role:create')" @click="openDialog()">新增角色</el-button>
        </div>
      </template>
      <el-table :data="filteredList" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleName" label="角色名称" />
        <el-table-column prop="roleCode" label="角色编码" />
        <el-table-column prop="description" label="描述" />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" v-if="userStore.hasPermission('sys:role:update')" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="warning" v-if="userStore.hasPermission('sys:role:assign')" @click="openMenuDialog(row)">分配菜单</el-button>
            <el-button size="small" type="danger" v-if="userStore.hasPermission('sys:role:delete')" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && filteredList.length === 0" description="暂无数据" />
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="如系统管理员" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="!!form.id" placeholder="如 ROLE_ADMIN" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单 -->
    <el-dialog v-model="menuDialogVisible" title="分配菜单权限" width="500px">
      <p style="margin-bottom: 12px">角色：{{ currentRole?.roleName }}</p>
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        show-checkbox
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        :default-checked-keys="selectedMenuIds"
        :check-strictly="true"
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="handleAssignMenus">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/api/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

interface Role {
  id: number
  roleCode: string
  roleName: string
  description: string
  createdAt: string
}

interface MenuNode {
  id: number
  name: string
  children?: MenuNode[]
}

const list = ref<Role[]>([])
const loading = ref(false)
const saveLoading = ref(false)
const assignLoading = ref(false)
const dialogVisible = ref(false)
const menuDialogVisible = ref(false)
const currentRole = ref<Role | null>(null)
const selectedMenuIds = ref<number[]>([])
const menuTree = ref<MenuNode[]>([])
const menuTreeRef = ref<any>(null)
const formRef = ref<FormInstance>()

const search = ref({ keyword: '' })
const pagination = ref({ page: 1, size: 10, total: 0 })
const form = ref<Partial<Role>>({})

const rules: FormRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const filteredList = computed(() => {
  if (!search.value.keyword) return list.value
  const kw = search.value.keyword.toLowerCase()
  return list.value.filter(
    r => r.roleCode.toLowerCase().includes(kw) || r.roleName.toLowerCase().includes(kw)
  )
})

async function fetchData() {
  loading.value = true
  try {
    const res: any = await request.get('/v1/rbac/roles', {
      params: { page: pagination.value.page, size: pagination.value.size }
    })
    list.value = res.data?.records || res.data || []
    pagination.value.total = res.data?.total || list.value.length
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '查询失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.value.page = 1
  fetchData()
}

function openDialog(row?: Role) {
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
      await request.put(`/v1/rbac/roles/${form.value.id}`, form.value)
      ElMessage.success('更新成功')
    } else {
      await request.post('/v1/rbac/roles', form.value)
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

async function handleDelete(row: Role) {
  try {
    await ElMessageBox.confirm('确认删除该角色？', '提示', { type: 'warning' })
    await request.delete(`/v1/rbac/roles/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '删除失败')
    }
  }
}

async function openMenuDialog(row: Role) {
  currentRole.value = row
  menuDialogVisible.value = true
  selectedMenuIds.value = []
  await nextTick()
  try {
    const res: any = await request.get(`/v1/rbac/roles/${row.id}`)
    const menus = res.data?.menus || []
    selectedMenuIds.value = extractIds(menus)
    if (menuTreeRef.value) {
      menuTreeRef.value.setCheckedKeys(selectedMenuIds.value)
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '获取菜单失败')
  }
}

function extractIds(nodes: MenuNode[]): number[] {
  const ids: number[] = []
  function walk(arr: MenuNode[]) {
    for (const n of arr) {
      ids.push(n.id)
      if (n.children?.length) walk(n.children)
    }
  }
  walk(nodes)
  return ids
}

async function handleAssignMenus() {
  if (!currentRole.value || !menuTreeRef.value) return
  const checkedKeys = menuTreeRef.value.getCheckedKeys()
  const halfKeys = menuTreeRef.value.getHalfCheckedKeys()
  const allKeys = [...checkedKeys, ...halfKeys]
  assignLoading.value = true
  try {
    await request.post(`/v1/rbac/roles/${currentRole.value.id}/menus`, allKeys)
    ElMessage.success('菜单分配成功')
    menuDialogVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '分配失败')
  } finally {
    assignLoading.value = false
  }
}

async function fetchMenus() {
  try {
    const res: any = await request.get('/v1/rbac/menus/tree')
    menuTree.value = res.data || []
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '获取菜单失败')
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
  fetchMenus()
})
</script>
