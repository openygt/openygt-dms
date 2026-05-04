<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>毒性药材清单管理</span>
          <el-button type="primary" @click="handleAdd">新增毒性药材</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="medicineName" label="药材名称" />
        <el-table-column prop="toxicityLevel" label="毒性等级">
          <template #default="{row}">
            <el-tag :type="row.toxicityLevel === 3 ? 'danger' : row.toxicityLevel === 2 ? 'warning' : 'info'">
              {{ toxicityLevelText(row.toxicityLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maxDosage" label="单次最大用量(g)" />
        <el-table-column prop="maxDailyDosage" label="每日最大用量(g)" />
        <el-table-column prop="washLevel" label="清洗级别">
          <template #default="{row}">
            {{ row.washLevel === 2 ? '强化' : '常规' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{row}">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="药材ID">
          <el-input v-model="form.medicineId" type="number" />
        </el-form-item>
        <el-form-item label="药材名称">
          <el-input v-model="form.medicineName" />
        </el-form-item>
        <el-form-item label="毒性等级">
          <el-select v-model="form.toxicityLevel">
            <el-option label="小毒" :value="1" />
            <el-option label="有毒" :value="2" />
            <el-option label="大毒" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="单次最大用量">
          <el-input v-model="form.maxDosage" type="number" />
        </el-form-item>
        <el-form-item label="每日最大用量">
          <el-input v-model="form.maxDailyDosage" type="number" />
        </el-form-item>
        <el-form-item label="清洗级别">
          <el-select v-model="form.washLevel">
            <el-option label="常规" :value="1" />
            <el-option label="强化" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增毒性药材')
const form = ref<any>({})
const isEdit = ref(false)

const toxicityLevelText = (level: number) => {
  const map: Record<number, string> = { 1: '小毒', 2: '有毒', 3: '大毒' }
  return map[level] || '未知'
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/v1/base/toxic-medicine')
    list.value = res.data.data?.records || []
  } catch (e) {
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增毒性药材'
  form.value = { washLevel: 1, toxicityLevel: 1, isActive: 1 }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  dialogTitle.value = '编辑毒性药材'
  form.value = { ...row }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (isEdit.value) {
      await request.put(`/v1/base/toxic-medicine/${form.value.id}`, form.value)
    } else {
      await request.post('/v1/base/toxic-medicine', form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认删除该毒性药材记录？', '提示', { type: 'warning' })
    await request.delete(`/v1/base/toxic-medicine/${row.id}`)
    ElMessage.success('删除成功')
    fetchList()
  } catch (e) {
    // cancel
  }
}

onMounted(fetchList)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
