<template>
  <div class="page-container">
    <div class="page-header-title">发药确认：<span class="page-header-sub">按处方/患者发药、签收确认</span></div>
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" clearable placeholder="全部" style="width: 140px" @change="handleSearch">
            <el-option label="待交付" value="PENDING" />
            <el-option label="已交付" value="DELIVERED" />
          </el-select>
        </el-form-item>
        <el-form-item label="交付方式">
          <el-select v-model="searchForm.deliveryType" clearable placeholder="全部" style="width: 140px" @change="handleSearch">
            <el-option label="自取" value="SELF" />
            <el-option label="快递" value="EXPRESS" />
            <el-option label="配送" value="DELIVERY" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" clearable placeholder="处方号/患者姓名" style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" data-testid="search-btn" @click="handleSearch">查询</el-button>
          <el-button type="success" data-testid="create-btn" @click="openDialog()">新增</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" data-testid="data-table">
        <el-table-column type="index" width="50" />
        <el-table-column prop="prescriptionNo" label="处方号" min-width="140" />
        <el-table-column prop="patientName" label="患者姓名" min-width="100" />
        <el-table-column prop="deliveryType" label="交付方式" min-width="100">
          <template #default="{ row }">
            <el-tag v-if="row.deliveryType === 'SELF'">自取</el-tag>
            <el-tag v-else-if="row.deliveryType === 'EXPRESS'" type="warning">快递</el-tag>
            <el-tag v-else-if="row.deliveryType === 'DELIVERY'" type="success">配送</el-tag>
            <span v-else>{{ row.deliveryType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="bagCount" label="袋数" min-width="80" />
        <el-table-column prop="receiverName" label="接收人" min-width="100" />
        <el-table-column prop="receiverPhone" label="联系电话" min-width="120" />
        <el-table-column label="操作" min-width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" link type="primary" @click="openConfirmDialog(row)">确认交付</el-button>
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          @change="handleSearch"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑交付记录' : '新增交付记录'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="处方号" prop="prescriptionNo">
          <el-input v-model="form.prescriptionNo" placeholder="请输入处方号" />
        </el-form-item>
        <el-form-item label="患者姓名" prop="patientName">
          <el-input v-model="form.patientName" placeholder="请输入患者姓名" />
        </el-form-item>
        <el-form-item label="交付方式" prop="deliveryType">
          <el-select v-model="form.deliveryType" placeholder="请选择交付方式" style="width: 100%">
            <el-option label="自取" value="SELF" />
            <el-option label="快递" value="EXPRESS" />
            <el-option label="配送" value="DELIVERY" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收人">
          <el-input v-model="form.receiverName" placeholder="请输入接收人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.receiverPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="接收地址">
          <el-input v-model="form.receiverAddress" placeholder="请输入接收地址" />
        </el-form-item>
        <el-form-item label="快递公司">
          <el-input v-model="form.courierCompany" placeholder="请输入快递公司" />
        </el-form-item>
        <el-form-item label="快递单号">
          <el-input v-model="form.courierNo" placeholder="请输入快递单号" />
        </el-form-item>
        <el-form-item label="袋数">
          <el-input-number v-model="form.bagCount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 确认交付弹窗 -->
    <el-dialog v-model="confirmVisible" title="确认交付" width="400px">
      <el-form ref="confirmFormRef" :model="confirmForm" :rules="confirmRules" label-width="80px">
        <el-form-item label="处方号">
          <span>{{ confirmForm.prescriptionNo }}</span>
        </el-form-item>
        <el-form-item label="接收人" prop="receiverName">
          <el-input v-model="confirmForm.receiverName" placeholder="请输入接收人姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="confirmForm.receiverPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="confirmForm.remark" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="confirmVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确认交付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getDeliveryRecordList,
  createDeliveryRecord,
  updateDeliveryRecord,
  deleteDeliveryRecord,
  confirmDelivery
} from '@/api/delivery'

const userStore = useUserStore()

interface DeliveryRecord {
  id: number
  taskId?: number
  prescriptionNo?: string
  patientName?: string
  deliveryType?: string
  receiverName?: string
  receiverPhone?: string
  receiverAddress?: string
  courierCompany?: string
  courierNo?: string
  bagCount?: number
  status?: string
  remark?: string
  deliveredAt?: string
}

const loading = ref(false)
const searchForm = reactive({
  status: '',
  deliveryType: '',
  keyword: ''
})
const tableData = ref<DeliveryRecord[]>([])
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const dialogVisible = ref(false)
const formRef = ref<any>(null)
const form = ref<Partial<DeliveryRecord>>({})
const rules = {
  prescriptionNo: [{ required: true, message: '请输入处方号', trigger: 'blur' }],
  patientName: [{ required: true, message: '请输入患者姓名', trigger: 'blur' }],
  deliveryType: [{ required: true, message: '请选择交付方式', trigger: 'change' }]
}

const confirmVisible = ref(false)
const confirmFormRef = ref<any>(null)
const confirmForm = reactive({
  id: 0,
  prescriptionNo: '',
  receiverName: '',
  receiverPhone: '',
  remark: ''
})
const confirmRules = {
  receiverName: [{ required: true, message: '请输入接收人姓名', trigger: 'blur' }]
}

async function handleSearch() {
  loading.value = true
  try {
    const res: any = await getDeliveryRecordList({
      page: pagination.page,
      size: pagination.size,
      status: searchForm.status || undefined,
      deliveryType: searchForm.deliveryType || undefined,
      keyword: searchForm.keyword || undefined
    })
    const data = res.data || {}
    tableData.value = data.records || []
    pagination.total = data.total || 0
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '查询失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

function openDialog(row?: DeliveryRecord) {
  form.value = row ? { ...row } : {}
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    if (form.value.id) {
      await updateDeliveryRecord(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createDeliveryRecord(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    handleSearch()
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '操作失败'
    ElMessage.error(msg)
  }
}

async function handleDelete(row: DeliveryRecord) {
  try {
    await ElMessageBox.confirm('确认删除该交付记录？', '提示', { type: 'warning' })
    await deleteDeliveryRecord(row.id)
    ElMessage.success('删除成功')
    handleSearch()
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '操作失败'
    if (msg !== '取消') ElMessage.error(msg)
  }
}

function openConfirmDialog(row: DeliveryRecord) {
  confirmForm.id = row.id
  confirmForm.prescriptionNo = row.prescriptionNo || ''
  confirmForm.receiverName = row.receiverName || ''
  confirmForm.receiverPhone = row.receiverPhone || ''
  confirmForm.remark = ''
  confirmVisible.value = true
}

async function handleConfirm() {
  const valid = await confirmFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    await confirmDelivery(confirmForm.id, {
      operatorId: userStore.userInfo?.username || 'admin',
      receiverName: confirmForm.receiverName,
      receiverPhone: confirmForm.receiverPhone,
      remark: confirmForm.remark
    })
    ElMessage.success('交付确认成功')
    confirmVisible.value = false
    handleSearch()
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '确认失败'
    ElMessage.error(msg)
  }
}

handleSearch()
</script>

<style scoped>
.page-container {
  padding: var(--ygt-space-4);
}
.search-card {
  margin-bottom: 16px;
}
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
