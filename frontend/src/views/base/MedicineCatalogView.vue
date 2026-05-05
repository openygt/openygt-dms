<template>
  <div>
    <div class="page-header-title">药材管理：<span class="page-header-sub">药材目录、库存关联</span></div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>药材目录</span>
          <el-button type="primary" data-testid="create-btn" @click="openDialog()">新增药材</el-button>
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
        <el-table-column prop="medicineCode" label="编码" width="120" />
        <el-table-column prop="medicineName" label="名称" />
        <el-table-column prop="hisCode" label="HIS编码" width="120" />
        <el-table-column prop="spec" label="规格" width="100" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="stockWarning" label="库存预警" width="100" />
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
      <el-form :model="form" label-width="100px">
        <el-form-item label="药材编码" required>
          <el-input v-model="form.medicineCode" />
        </el-form-item>
        <el-form-item label="药材名称" required>
          <el-input v-model="form.medicineName" />
        </el-form-item>
        <el-form-item label="别名">
          <el-input v-model="form.aliases" />
        </el-form-item>
        <el-form-item label="HIS编码" required>
          <el-input v-model="form.hisCode" />
        </el-form-item>
        <el-form-item label="国标编码">
          <el-input v-model="form.nationalCode" />
        </el-form-item>
        <el-form-item label="规格" required>
          <el-input v-model="form.spec" />
        </el-form-item>
        <el-form-item label="单位" required>
          <el-input v-model="form.unit" />
        </el-form-item>
        <el-form-item label="库存预警">
          <el-input v-model.number="form.stockWarning" type="number" />
        </el-form-item>
        <el-form-item label="状态" required>
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button data-testid="dialog-cancel-btn" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" data-testid="dialog-save-btn" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMedicineList,
  createMedicine,
  updateMedicine,
  deleteMedicine
} from '@/api/baseData'

interface Medicine {
  id: number
  medicineCode: string
  medicineName: string
  aliases: string
  hisCode: string
  nationalCode: string
  spec: string
  unit: string
  stockWarning: number
  status: number
}

const list = ref<Medicine[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

const search = reactive({ keyword: '' })
const form = ref<Partial<Medicine>>({ status: 1 })

async function fetchData() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (search.keyword) params.keyword = search.keyword
    const res: any = await getMedicineList(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
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
  form.value = row ? { ...row } : { status: 1 }
  dialogVisible.value = true
}

async function handleSave() {
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
