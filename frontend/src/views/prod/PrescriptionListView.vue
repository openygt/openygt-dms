<template>
  <div>
    <div class="page-header-title">处方录入：<span class="page-header-sub">手工录入处方、导入、查询和异常处方处理</span></div>
    <div style="height: 16px"></div>

    <!-- 操作栏 -->
    <el-card>
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
        <el-radio-group v-model="activeTab" @change="handleTabChange">
          <el-radio-button value="list">处方列表</el-radio-button>
          <el-radio-button value="exceptions">异常处方({{ exceptionCount }})</el-radio-button>
        </el-radio-group>
        <div v-if="activeTab === 'list'">
          <el-button type="primary" @click="openCreateDialog">新增处方</el-button>
          <el-button @click="importDialogVisible = true">CSV导入</el-button>
          <el-button @click="ocrDialogVisible = true">OCR识别</el-button>
        </div>
      </div>

      <!-- 搜索 -->
      <el-form v-if="activeTab === 'list'" :inline="true" @submit.prevent>
        <el-form-item label="医院">
          <el-select v-model="search.hospitalId" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="h in hospitals" :key="h.id" :label="h.name" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="search.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="待处理" value="待处理" />
            <el-option label="处理中" value="处理中" />
            <el-option label="处理完毕" value="处理完毕" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker v-model="search.dateRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item label="搜索">
          <el-input v-model="search.keyword" placeholder="患者姓名" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 处方列表 -->
    <el-card style="margin-top: 12px">
      <el-table v-if="activeTab === 'list'" :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="处方号" width="80" />
        <el-table-column label="医院"><template #default="{row}">{{ hospitalMap[row.hospitalId] || '-' }}</template></el-table-column>
        <el-table-column prop="patientName" label="患者姓名" />
        <el-table-column label="类型" width="80"><template #default="{row}">{{ row.patientType === 1 ? '住院' : '门诊' }}</template></el-table-column>
        <el-table-column prop="doctorName" label="医师" width="100" />
        <el-table-column prop="repetition" label="付数" width="60" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{row}"><el-tag :type="statusType(row.status)">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="创建时间" width="160"><template #default="{row}">{{ formatTime(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openCreateDialog(row)">编辑</el-button>
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 异常处方列表 -->
      <el-table v-else :data="exceptionList" v-loading="exceptionLoading" border>
        <el-table-column prop="id" label="处方号" width="80" />
        <el-table-column prop="patientName" label="患者姓名" />
        <el-table-column label="来源" width="80"><template #default="{row}">{{ row.source || '-' }}</template></el-table-column>
        <el-table-column prop="exceptionReason" label="异常原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="repetition" label="付数" width="60" />
        <el-table-column label="时间" width="160"><template #default="{row}">{{ formatTime(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openCreateDialog(row)">编辑</el-button>
            <el-button size="small" type="warning" @click="resolveException(row)">纠正</el-button>
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="activeTab === 'list' && total > 0" v-model:current-page="page" v-model:page-size="size" :total="total"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" style="margin-top: 16px"
        @size-change="fetchData" @current-change="fetchData" />
      <el-pagination v-if="activeTab === 'exceptions' && exceptionTotal > 0" v-model:current-page="exceptionPage" v-model:page-size="exceptionSize" :total="exceptionTotal"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" style="margin-top: 16px"
        @size-change="fetchExceptions" @current-change="fetchExceptions" />
    </el-card>

    <!-- ==================== 新增处方对话框 ==================== -->
    <el-dialog v-model="createDialogVisible" :title="form.id ? '编辑处方' : '新增处方'" width="800px">
      <el-form :model="form" :rules="formRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="患者姓名" prop="patientName">
              <el-input v-model="form.patientName" placeholder="必填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="医院" prop="hospitalId" required>
              <el-select v-model="form.hospitalId" placeholder="请选择" clearable style="width:100%">
                <el-option v-for="h in hospitals" :key="h.id" :label="h.name" :value="h.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="患者类型">
              <el-radio-group v-model="form.patientType">
                <el-radio :label="0">门诊</el-radio>
                <el-radio :label="1">住院</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="医师" prop="doctorName" required>
              <el-input v-model="form.doctorName" placeholder="必填" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="付数">
              <el-input-number v-model="form.repetition" :min="1" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="煎煮方案">
              <el-select v-model="form.schemeId" placeholder="默认" clearable style="width:100%">
                <el-option v-for="s in schemes" :key="s.id" :label="s.schemeName || s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" rows="2" />
        </el-form-item>

        <!-- 药材明细 -->
        <el-form-item label="药材明细">
          <el-button type="primary" size="small" @click="addMedicineRow">+ 添加药材</el-button>
          <el-button size="small" @click="addMedicineBatch" v-if="form.medicineItems.length === 0">批量粘贴</el-button>
        </el-form-item>
        <el-table :data="form.medicineItems" border size="small" max-height="400">
          <el-table-column label="药材名称" width="200">
            <template #default="{ row, $index }">
              <el-autocomplete v-model="row.medicineName" :fetch-suggestions="queryMedicines"
                :trigger-on-focus="false" placeholder="搜索或直接输入" value-key="medicineName"
                @select="(e: any) => onMedicineSelect(e, row)" clearable />
            </template>
          </el-table-column>
          <el-table-column label="用量" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.dosage" :min="0.1" :step="1" :precision="2" size="small"
                controls-position="right" style="width:110px" />
            </template>
          </el-table-column>
          <el-table-column label="单位" width="70">
            <template #default="{ row }">
              <el-input v-model="row.unit" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="煎法" width="110">
            <template #default="{ row }">
              <el-select v-model="row.decoctionMethod" placeholder="常规" clearable size="small" style="width:100%">
                <el-option label="常规" value="NORMAL" /><el-option label="先煎" value="DECOCT_FIRST" />
                <el-option label="后下" value="ADD_LATE" /><el-option label="包煎" value="WRAP_DECOCT" />
                <el-option label="另煎" value="SEPARATE_DECOCT" /><el-option label="冲服" value="INFUSE" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="毒" width="55" align="center">
            <template #default="{ row }">
              <el-checkbox v-model="row.isToxic" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="60" fixed="right">
            <template #default="{ row, $index }">
              <el-button type="danger" size="small" link @click="removeMedicineRow($index)">×</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!canSubmit" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- ==================== CSV导入对话框 ==================== -->
    <el-dialog v-model="importDialogVisible" title="CSV导入处方" width="500px">
      <div style="margin-bottom: 12px">
        <p style="color:#666;font-size:13px">CSV格式：患者姓名,药材名称,用量,单位,煎法,付数,备注</p>
        <p style="color:#666;font-size:13px">同一患者多行药材会自动合并为一个处方</p>
      </div>
      <el-upload drag accept=".csv" :auto-upload="false" :show-file-list="true"
        :on-change="handleImportFileChange" :limit="1">
        <el-icon style="font-size:48px;color:#409eff"><UploadFilled /></el-icon>
        <div>将CSV文件拖到此处，或<em>点击选择</em></div>
      </el-upload>
      <el-form :inline="true" style="margin-top:12px">
        <el-form-item label="医院">
          <el-select v-model="importHospitalId" placeholder="全部" clearable style="width:160px">
            <el-option v-for="h in hospitals" :key="h.id" :label="h.name" :value="h.id" />
          </el-select>
        </el-form-item>

      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!importFile" :loading="importing" @click="handleImport">导入</el-button>
      </template>
    </el-dialog>

    <!-- ==================== OCR对话框 ==================== -->
    <el-dialog v-model="ocrDialogVisible" title="OCR识别处方" width="600px">
      <el-upload drag accept=".jpg,.jpeg,.png,.webp" :auto-upload="false" :show-file-list="false"
        :on-change="handleOcrImageChange">
        <el-icon style="font-size:48px;color:#409eff"><UploadFilled /></el-icon>
        <div>上传处方图片</div>
      </el-upload>
      <div v-if="ocrImageUrl" style="margin-top:12px">
        <el-image :src="ocrImageUrl" style="max-width:100%;max-height:300px" fit="contain" />
      </div>
      <div v-if="ocrResult" style="margin-top:12px">
        <el-form :model="ocrResult" label-width="80px">
          <el-form-item label="患者姓名">
            <el-input v-model="ocrResult.patientName" />
          </el-form-item>
          <el-form-item label="付数">
            <el-input-number v-model="ocrResult.repetition" :min="1" />
          </el-form-item>
        </el-form>
        <h4>识别药材（请核对修正）</h4>
        <el-table :data="ocrResult.items" border size="small">
          <el-table-column label="药材名称">
            <template #default="{row}"><el-input v-model="row.medicineName" size="small" /></template>
          </el-table-column>
          <el-table-column label="用量" width="120">
            <template #default="{row}"><el-input v-model="row.dosage" size="small" /></template>
          </el-table-column>
          <el-table-column label="单位" width="70">
            <template #default="{row}"><el-input v-model="row.unit" size="small" /></template>
          </el-table-column>
          <el-table-column label="操作" width="60">
            <template #default="{row,$index}">
              <el-button type="danger" size="small" link @click="ocrResult.items.splice($index,1)">×</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button size="small" style="margin-top:8px"
          @click="ocrResult.items.push({medicineName:'',dosage:'',unit:'g'})">+ 添加一行</el-button>
      </div>
      <template #footer>
        <el-button @click="ocrDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!ocrResult" :loading="ocrSubmitting" @click="handleOcrConfirm">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 详情抽屉 ==================== -->
    <el-drawer v-model="detailVisible" title="处方详情" size="600px">
      <div v-if="detail" v-loading="detailLoading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="处方号">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="患者">{{ detail.patientName }}</el-descriptions-item>
          <el-descriptions-item label="医院">{{ hospitalMap[detail.hospitalId] || '-' }}</el-descriptions-item>
          <el-descriptions-item label="医师">{{ detail.doctorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ detail.patientType === 1 ? '住院' : '门诊' }}</el-descriptions-item>
          <el-descriptions-item label="付数">{{ detail.repetition }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="receiveStatusType(detail.receiveStatus)">{{ receiveStatusLabel(detail.receiveStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.importException" label="异常" :span="2">
            <el-tag type="danger">异常处方</el-tag>
            <span style="margin-left:8px;color:#e74c3c">{{ detail.exceptionReason }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <h4 style="margin-top:16px">药材明细</h4>
        <el-table :data="detail.medicineItems || []" border size="small">
          <el-table-column prop="medicineName" label="药材名称" />
          <el-table-column prop="dosage" label="剂量" width="100" />
          <el-table-column prop="unit" label="单位" width="60" />
          <el-table-column label="煎法" width="80">
            <template #default="{row}">{{ decoctionMethodLabel(row.decoctionMethod) }}</template>
          </el-table-column>
          <el-table-column label="毒" width="55" align="center">
            <template #default="{row}">{{ row.isToxic ? '是' : '否' }}</template>
          </el-table-column>
        </el-table>
        <div v-if="detail.rawImportData" style="margin-top:12px">
          <el-button size="small" @click="showRawData = !showRawData">
            {{ showRawData ? '收起' : '查看' }}原始导入数据
          </el-button>
          <pre v-if="showRawData" style="background:#f5f5f5;padding:8px;margin-top:8px;font-size:12px;max-height:200px;overflow:auto">{{ formatJson(detail.rawImportData) }}</pre>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import request from '@/api/request'
import {
  listPrescriptions, createStructuredPrescription, updatePrescription,
  searchMedicines, importPrescriptionCsv,
  createPrescriptionFromOcr, uploadOcrImage,
  getPrescriptionDetail
} from '@/api/prescription'

// ==================== 数据结构 ====================

interface MedicineItem {
  medicineId: number | null
  medicineName: string
  dosage: number
  unit: string
  decoctionMethod: string
  batchNo: string
  isToxic: boolean
  [key: string]: any
}

const defaultMedicineItem = (): MedicineItem => ({
  medicineId: null, medicineName: '', dosage: 10, unit: 'g',
  decoctionMethod: 'NORMAL', batchNo: '', isToxic: false
})

// ==================== 状态 ====================

const activeTab = ref('list')
const loading = ref(false)
const exceptionLoading = ref(false)
const exceptionCount = ref(0)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const list = ref<any[]>([])
const exceptionList = ref<any[]>([])
const exceptionPage = ref(1)
const exceptionSize = ref(10)
const exceptionTotal = ref(0)

const search = reactive({
  hospitalId: undefined as number | undefined,
  status: '',
  keyword: '',
  dateRange: null as [string, string] | null
})

// 主从数据
const schemes = ref<any[]>([])
const hospitals = ref<any[]>([])
const hospitalMap = ref<Record<number, string>>({})
const schemeMap = ref<Record<number, string>>({})

// 创建对话框
const createDialogVisible = ref(false)
const form = reactive<any>({
  patientName: '', hospitalId: null, patientType: 0,
  doctorName: '', schemeId: null, repetition: 1,
  remark: '', medicineItems: [] as MedicineItem[]
})

const formRules = {
  patientName: [{ required: true, message: '请输入患者姓名', trigger: 'blur' }],
  hospitalId: [{ required: true, message: '请选择医院', trigger: 'change' }],
  doctorName: [{ required: true, message: '请输入医师姓名', trigger: 'blur' }]
}

// 导入
const importDialogVisible = ref(false)
const importFile = ref<File | null>(null)
const importHospitalId = ref<number | undefined>()

const importing = ref(false)

// OCR
const ocrDialogVisible = ref(false)
const ocrImageUrl = ref('')
const ocrResult = ref<any>(null)
const ocrSubmitting = ref(false)

// 详情
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)
const showRawData = ref(false)

// ==================== 计算属性 ====================

const canSubmit = computed(() => {
  if (!form.patientName) return false
  if (!form.medicineItems.length) return false
  return form.medicineItems.every((m: MedicineItem) => m.medicineName && m.dosage > 0)
})

// ==================== 数据加载 ====================

async function fetchData() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (search.hospitalId != null) params.hospitalId = search.hospitalId
    if (search.status) params.status = search.status
    if (search.keyword) params.keyword = search.keyword
    if (search.dateRange && search.dateRange[0]) {
      params.startTime = search.dateRange[0]
      params.endTime = search.dateRange[1]
    }
    const res: any = await listPrescriptions(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function fetchExceptions() {
  exceptionLoading.value = true
  try {
    const res: any = await request.get('/v1/prod/prescriptions/exceptions', {
      params: { page: exceptionPage.value, size: exceptionSize.value }
    })
    exceptionList.value = res.data?.records || []
    exceptionTotal.value = res.data?.total || 0
    exceptionCount.value = res.data?.total || 0
  } finally {
    exceptionLoading.value = false
  }
}

function handleTabChange() {
  if (activeTab.value === 'exceptions') fetchExceptions()
}

function handleQuery() { page.value = 1; fetchData() }
function resetSearch() {
  search.hospitalId = undefined; search.status = ''; search.keyword = ''; search.dateRange = null
  page.value = 1; fetchData()
}

function statusType(s?: string) {
  const map: Record<string, string> = { '待处理': 'info', '处理中': 'warning', '处理完毕': 'success' }
  return map[s || ''] || ''
}

function receiveStatusLabel(s?: string) {
  const map: Record<string, string> = { PENDING: '待接收', RECEIVED: '已接收', REJECTED: '已拒收' }
  return map[s || ''] || s || '-'
}

function receiveStatusType(s?: string) {
  const map: Record<string, string> = { PENDING: 'info', RECEIVED: 'success', REJECTED: 'danger' }
  return map[s || ''] || ''
}

function decoctionMethodLabel(s?: string) {
  const map: Record<string, string> = {
    NORMAL: '常规', DECOCT_FIRST: '先煎', ADD_LATE: '后下',
    WRAP_DECOCT: '包煎', SEPARATE_DECOCT: '另煎', INFUSE: '冲服'
  }
  return map[s || ''] || s || '-'
}

function formatTime(dt: string) {
  if (!dt) return '-'
  const d = new Date(dt)
  return isNaN(d.getTime()) ? dt : d.toLocaleString('zh-CN')
}

// ==================== 创建处方 ====================

function openCreateDialog(row?: any) {
  if (row) {
    // 编辑模式：预填充数据
    form.id = row.id
    form.patientName = row.patientName || ''
    form.hospitalId = row.hospitalId || null
    form.patientType = row.patientType ?? 0
    form.doctorName = row.doctorName || ''
    form.schemeId = row.schemeId || null
    form.repetition = row.repetition || 1
    form.remark = row.remark || ''
    form.medicineItems = row.medicineItems?.map((m: any) => ({
      medicineId: m.medicineId || null,
      medicineName: m.medicineName || '',
      dosage: m.dosage || 10,
      unit: m.unit || 'g',
      decoctionMethod: m.decoctionMethod || 'NORMAL',
      batchNo: m.batchNo || '',
      isToxic: !!m.isToxic
    })) || []
  } else {
    // 新增模式：清空表单
    form.id = undefined
    form.patientName = ''; form.hospitalId = null; form.patientType = 0
    form.doctorName = ''; form.schemeId = null; form.repetition = 1
    form.remark = ''; form.medicineItems = []
  }
  createDialogVisible.value = true
}

function addMedicineRow() { form.medicineItems.push(defaultMedicineItem()) }
function removeMedicineRow(index: number) { form.medicineItems.splice(index, 1) }

function addMedicineBatch() {
  ElMessageBox.prompt('按行粘贴，格式：药材名称,用量,单位\n如：\n当归,15,g\n川芎,10,g', '批量粘贴', {
    inputType: 'textarea', inputPlaceholder: '药材名称,用量,单位'
  }).then(({ value }) => {
    if (!value) return
    value.split('\n').filter(l => l.trim()).forEach(line => {
      const parts = line.split(/[,，\t]+/).map(s => s.trim())
      if (parts.length >= 2) {
        form.medicineItems.push({
          medicineId: null,
          medicineName: parts[0],
          dosage: parseFloat(parts[1]) || 10,
          unit: parts[2] || 'g',
          decoctionMethod: 'NORMAL', batchNo: '', isToxic: false
        })
      }
    })
  }).catch(() => {})
}

// 药品自动补全
async function queryMedicines(query: string, cb: (results: any[]) => void) {
  if (!query || query.length < 1) { cb([]); return }
  try {
    const res: any = await searchMedicines(query)
    cb((res.data?.records || []).map((m: any) => ({ ...m, value: m.medicineName })))
  } catch { cb([]) }
}

function onMedicineSelect(selected: any, row: any) {
  row.medicineId = selected.id
  row.medicineName = selected.medicineName
  row.unit = selected.unit || 'g'
}

async function handleSave() {
  try {
    const payload = {
      patientName: form.patientName,
      hospitalId: form.hospitalId,
      patientType: form.patientType,
      doctorName: form.doctorName,
      schemeId: form.schemeId,
      repetition: form.repetition,
      remark: form.remark,
      source: 'MANUAL',
      medicineItems: form.medicineItems.map((item: MedicineItem, idx: number) => ({
        medicineId: item.medicineId,
        medicineName: item.medicineName,
        dosage: item.dosage,
        unit: item.unit,
        decoctionMethod: item.decoctionMethod,
        sortOrder: idx + 1,
        batchNo: item.batchNo || null
      }))
    }
    if (form.id) {
      await updatePrescription(form.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createStructuredPrescription(payload)
      ElMessage.success('创建成功')
    }
    createDialogVisible.value = false
    fetchData()
  } catch { /* handled by interceptor */ }
}

// ==================== CSV导入 ====================

function handleImportFileChange(file: any) {
  importFile.value = file.raw || file
}

async function handleImport() {
  if (!importFile.value) return
  importing.value = true
  try {
    const res: any = await importPrescriptionCsv(
      importFile.value, importHospitalId.value)
    ElMessage.success(`导入完成：${res.data?.length || 0} 条处方`)
    importDialogVisible.value = false
    importFile.value = null
    fetchData()
  } catch { /* handled by interceptor */ }
  finally { importing.value = false }
}

// ==================== OCR ====================

async function handleOcrImageChange(file: any) {
  const raw = file.raw || file
  if (!raw) return
  try {
    const res: any = await uploadOcrImage(raw)
    ocrImageUrl.value = res.data || ''
    // 模拟OCR识别结果（实际应调用OCR服务）
    ocrResult.value = {
      patientName: '', repetition: 1, imageUrl: ocrImageUrl.value,
      items: [{ medicineName: '', dosage: '', unit: 'g' }]
    }
    ElMessage.success('图片上传成功，请填写识别结果')
  } catch { /* handled */ }
}

async function handleOcrConfirm() {
  if (!ocrResult.value) return
  ocrSubmitting.value = true
  try {
    await createPrescriptionFromOcr(ocrResult.value)
    ElMessage.success('处方创建成功')
    ocrDialogVisible.value = false
    ocrResult.value = null
    ocrImageUrl.value = ''
    fetchData()
  } finally { ocrSubmitting.value = false }
}

// ==================== 异常处方处理 ====================

async function resolveException(row: any) {
  try {
    await ElMessageBox.confirm(`确认纠正异常处方 #${row.id}？将在当前数据基础上修正`, '提示')
    // 加载完整数据
    const res: any = await getPrescriptionDetail(row.id)
    const detail = res.data
    if (!detail) { ElMessage.error('加载失败'); return }

    form.patientName = detail.patientName
    form.hospitalId = detail.hospitalId
    form.patientType = detail.patientType
    form.doctorName = detail.doctorName
    form.schemeId = detail.schemeId
    form.repetition = detail.repetition || 1
    form.remark = detail.remark || ''
    form.medicineItems = (detail.medicineItems || []).map((m: any) => ({
      medicineId: m.medicineId || null,
      medicineName: m.medicineName || '',
      dosage: m.dosage || 10,
      unit: m.unit || 'g',
      decoctionMethod: m.decoctionMethod || 'NORMAL',
      batchNo: m.batchNo || '',
      isToxic: !!m.isToxic
    }))
    if (form.medicineItems.length === 0) form.medicineItems.push(defaultMedicineItem())

    // 保存纠正
    await request.post(`/v1/prod/prescriptions/${row.id}/resolve-exception`, {
      patientName: form.patientName,
      hospitalId: form.hospitalId,
      patientType: form.patientType,
      doctorName: form.doctorName,
      schemeId: form.schemeId,
      repetition: form.repetition,
      remark: form.remark,
      source: 'MANUAL',
      medicineItems: form.medicineItems.map((item: any, idx: number) => ({
        medicineId: item.medicineId, medicineName: item.medicineName,
        dosage: item.dosage, unit: item.unit,
        decoctionMethod: item.decoctionMethod, sortOrder: idx + 1
      }))
    })
    ElMessage.success('异常处方已纠正')
    fetchExceptions()
    fetchData()
  } catch { /* handled */ }
}

// ==================== 详情 ====================

async function viewDetail(row: any) {
  detailVisible.value = true
  detailLoading.value = true
  showRawData.value = false
  try {
    const res: any = await getPrescriptionDetail(row.id)
    detail.value = res.data || null
  } catch { detail.value = row }
  finally { detailLoading.value = false }
}

function formatJson(str: string) {
  if (!str) return ''
  try { return JSON.stringify(JSON.parse(str), null, 2) } catch { return str }
}

// ==================== 初始化 ====================

async function fetchSchemes() {
  try {
    const res: any = await request.get('/v1/md/schemes', { params: { page: 1, size: 999 } })
    const list = res.data?.records || []
    schemes.value = list
    schemeMap.value = Object.fromEntries(list.map((s: any) => [s.id, s.schemeName || s.name]))
  } catch {}
}

async function fetchHospitals() {
  try {
    const res: any = await request.get('/v1/md/hospitals', { params: { page: 1, size: 999 } })
    const list = res.data?.records || []
    const enabled = list.filter((h: any) => h.status === 1)
    hospitals.value = enabled
    hospitalMap.value = Object.fromEntries(enabled.map((h: any) => [h.id, h.name]))
  } catch {}
}

onMounted(() => { fetchData(); fetchExceptions(); fetchSchemes(); fetchHospitals() })
</script>
