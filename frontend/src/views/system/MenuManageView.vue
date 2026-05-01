<template>
  <div>
    <div class="page-header-title">菜单管理：<span class="page-header-sub">菜单结构、显示顺序、权限绑定</span></div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>菜单管理</span>
          <el-button type="primary" @click="openDialog()">新增菜单</el-button>
        </div>
      </template>
      <el-table :data="flatList" v-loading="loading" row-key="id" border default-expand-all>
        <el-table-column prop="name" label="菜单名称" />
        <el-table-column prop="path" label="路由路径" />
        <el-table-column prop="permission" label="权限标识" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑菜单' : '新增菜单'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="treeData"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            clearable
            placeholder="不选则为一级菜单"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="路由路径">
          <el-input v-model="form.path" placeholder="如 /tasks" />
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input v-model="form.permission" placeholder="如 prod:task:list" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
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
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

interface MenuNode {
  id: number
  name: string
  path?: string
  permission?: string
  parentId?: number
  sort: number
  children?: MenuNode[]
}

const treeData = ref<MenuNode[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const form = ref<Partial<MenuNode>>({ sort: 0 })

const flatList = computed(() => {
  const result: MenuNode[] = []
  function walk(nodes: MenuNode[]) {
    for (const n of nodes) {
      result.push(n)
      if (n.children?.length) walk(n.children)
    }
  }
  walk(treeData.value)
  return result
})

async function fetchData() {
  loading.value = true
  try {
    const res: any = await request.get('/v1/rbac/menus/tree')
    treeData.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openDialog(row?: MenuNode) {
  form.value = row ? { ...row } : { sort: 0 }
  dialogVisible.value = true
}

async function handleSave() {
  try {
    if (form.value.id) {
      await request.put(`/v1/rbac/menus/${form.value.id}`, form.value)
      ElMessage.success('更新成功')
    } else {
      await request.post('/v1/rbac/menus', form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {}
}

async function handleDelete(row: MenuNode) {
  try {
    await ElMessageBox.confirm('确认删除该菜单？', '提示', { type: 'warning' })
    await request.delete(`/v1/rbac/menus/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {}
}

onMounted(fetchData)
</script>
