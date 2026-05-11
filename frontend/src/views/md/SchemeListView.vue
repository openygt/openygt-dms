<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>煎药方案</span>
          <el-button type="primary" v-if="userStore.hasPermission('md:scheme:create')" @click="openDialog()">新增方案</el-button>
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
        <el-table-column prop="soakTime" label="浸泡(分)" width="90" />
        <el-table-column prop="firstDecoctTime" label="一煎(分)" width="90" />
        <el-table-column prop="secondDecoctTime" label="二煎(分)" width="90" />
        <el-table-column prop="drainTime" label="出液(分)" width="90" />
        <el-table-column prop="packageTime" label="包装(分)" width="90" />
        <el-table-column prop="tempRange" label="温度范围" width="120" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" v-if="userStore.hasPermission('md:scheme:update')" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" v-if="userStore.hasPermission('md:scheme:delete')" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.total"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 16px; justify-content: flex-end;"
      />
      <el-empty v-if="!loading && list.length === 0" description="暂无方案" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑方案' : '新增方案'" width="600px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="140px">
        <el-form-item label="方案名称" prop="schemeName">
          <el-input v-model="form.schemeName" />
        </el-form-item>
        <el-form-item label="方案编码" prop="schemeCode">
          <el-input v-model="form.schemeCode" :disabled="!!form.id" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="浸泡时间(分)">
              <el-input-number v-model="form.soakTime" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="一煎时间(分)">
              <el-input-number v-model="form.firstDecoctTime" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="二煎时间(分)">
              <el-input-number v-model="form.secondDecoctTime" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出液时间(分)">
              <el-input-number v-model="form.drainTime" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="包装时间(分)">
              <el-input-number v-model="form.packageTime" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="后下提醒(分)">
              <el-input-number v-model="form.lateAddRemindTime" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="升温速率(°C/min)">
              <el-input-number v-model="form.tempRiseRate" :min="0" :precision="1" :step="0.5" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否默认方案">
              <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="低温阈值(°C)">
              <el-input-number v-model="form.alarmLowTemp" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="高温阈值(°C)">
              <el-input-number v-model="form.alarmHighTemp" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
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
        <el-button type="primary" :loading="saveLoading" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSchemeList, createScheme, updateScheme, deleteScheme } from '@/api/equipment'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

interface Scheme {
  id: number
  schemeName: string
  schemeCode: string
  schemeType: number
  decoctTimes: number
  pressure: number
  upperWater: number
  soakTime: number
  firstDecoctTime: number
  secondDecoctTime: number
  drainTime: number
  packageTime: number
  lateAddRemindTime: number
  tempRiseRate: number
  isDefault: number
  tempRange: string
  alarmLowTemp: number
  alarmHighTemp: number
  status: number
  remark: string
}

const list = ref<Scheme[]>([])
const loading = ref(false)
const saveLoading = ref(false)
const dialogVisible = ref(false)
const search = ref({ name: '' })
const form = ref<Partial<Scheme>>({ status: 1, isDefault: 0, alarmLowTemp: undefined, alarmHighTemp: undefined })
const formRef = ref<any>(null)
const pagination = ref({ page: 1, size: 10, total: 0 })

const formRules = {
  schemeName: [{ required: true, message: '方案名称不能为空', trigger: 'blur' }],
  schemeCode: [{ required: true, message: '方案编码不能为空', trigger: 'blur' }],
  firstDecoctTime: [{ required: true, message: '一煎时长不能为空', trigger: 'change' }],
  secondDecoctTime: [{ required: true, message: '二煎时长不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }],
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.value.page,
      size: pagination.value.size
    }
    if (search.value.name) params.keyword = search.value.name
    const res: any = await getSchemeList(params)
    list.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '查询失败')
  } finally {
    loading.value = false
  }
}

function handleSizeChange(val: number) {
  pagination.value.size = val
  pagination.value.page = 1
  fetchData()
}

function handlePageChange(val: number) {
  pagination.value.page = val
  fetchData()
}

function openDialog(row?: Scheme) {
  if (row) {
    form.value = { ...row }
  } else {
    form.value = { status: 1, isDefault: 0, alarmLowTemp: undefined, alarmHighTemp: undefined }
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saveLoading.value = true
  try {
    const totalHeatingTime = (form.value.firstDecoctTime || 0) + (form.value.secondDecoctTime || 0)
    const payload = {
      name: form.value.schemeName,
      code: form.value.schemeCode,
      schemeType: form.value.schemeType,
      decoctTimes: form.value.decoctTimes,
      pressure: form.value.pressure,
      upperWater: form.value.upperWater,
      description: form.value.remark,
      preHeatingTime: form.value.soakTime,
      heatingTime: totalHeatingTime > 0 ? totalHeatingTime : undefined,
      firstDecoctTime: form.value.firstDecoctTime,
      secondDecoctTime: form.value.secondDecoctTime,
      soakTime: form.value.soakTime,
      drainTime: form.value.drainTime,
      packageTime: form.value.packageTime,
      lateAddRemindTime: form.value.lateAddRemindTime,
      tempRiseRate: form.value.tempRiseRate,
      isDefault: form.value.isDefault,
      alarmLowTemp: form.value.alarmLowTemp,
      alarmHighTemp: form.value.alarmHighTemp,
      status: form.value.status,
    }
    if (form.value.id) {
      await updateScheme(form.value.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createScheme(payload)
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

async function handleDelete(row: Scheme) {
  try {
    await ElMessageBox.confirm('确认删除该方案？', '提示', { type: 'warning' })
    await deleteScheme(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '删除失败')
    }
  }
}

onMounted(fetchData)
</script>
