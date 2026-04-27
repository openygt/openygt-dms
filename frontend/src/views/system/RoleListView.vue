<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>角色管理</span>
          <el-button type="primary" @click="openDialog()">新增角色</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleCode" label="角色编码" />
        <el-table-column prop="roleName" label="角色名称" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="warning" @click="openMenuDialog(row)">分配菜单</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色编码" required>
          <el-input v-model="form.roleCode" :disabled="!!form.id" placeholder="如 ROLE_ADMIN" />
        </el-form-item>
        <el-form-item label="角色名称" required>
          <el-input v-model="form.roleName" placeholder="如系统管理员" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
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
        <el-button type="primary" @click="handleAssignMenus">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

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
const dialogVisible = ref(false)
const menuDialogVisible = ref(false)
const currentRole = ref<Role | null>(null)
const selectedMenuIds = ref<number[]>([])
const menuTree = ref<MenuNode[]>([])
const menuTreeRef = ref<any>(null)

const pagination = ref({ page: 1, size: 10, total: 0 })
const form = ref<Partial<Role>>({})

async function fetchData() {
  loading.value = true
  try {
    const res: any = await request.get('/v1/rbac/roles', {
      params: { page: pagination.value.page, size: pagination.value.size }
    })
    list.value = res.data?.records || res.data || []
    pagination.value.total = res.data?.total || list.value.length
  } finally {
    loading.value = false
  }
}

function openDialog(row?: Role) {
  form.value = row ? { ...row } : {}
  dialogVisible.value = true
}

async function handleSave() {
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
  } catch (e) {}
}

async function handleDelete(row: Role) {
  try {
    await ElMessageBox.confirm('确认删除该角色？', '提示', { type: 'warning' })
    await request.delete(`/v1/rbac/roles/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {}
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
  } catch (e) {}
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
  try {
    await request.post(`/v1/rbac/roles/${currentRole.value.id}/menus`, allKeys)
    ElMessage.success('菜单分配成功')
    menuDialogVisible.value = false
  } catch (e) {}
}

async function fetchMenus() {
  try {
    const res: any = await request.get('/v1/rbac/menus/tree')
    menuTree.value = res.data || []
  } catch (e) {}
}

onMounted(() => {
  fetchData()
  fetchMenus()
})
</script>
