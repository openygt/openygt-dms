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
          <el-button type="primary" @click="openCreateDialog()">新增处方</el-button>
          <el-button @click="importDialogVisible = true">CSV导入</el-button>
          <el-button disabled title="OCR识别功能开发中" @click="ocrDialogVisible = true">OCR识别</el-button>
        </div>
      </div>

      <!-- 搜索 -->
      <el-form v-if="activeTab === 'list'" :inline="true" @submit.prevent>
        <el-form-item label="处方号">
          <el-input v-model="search.prescriptionNumber" placeholder="处方号" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="患者姓名">
          <el-input v-model="search.patientName" placeholder="患者姓名" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="患者手机号">
          <el-input v-model="search.patientPhone" placeholder="患者手机号" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="search.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="处理完毕" value="COMPLETED" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker v-model="search.dateRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 240px" />
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
        <el-table-column prop="prescriptionNumber" label="处方号" width="100" />
        <el-table-column prop="patientName" label="患者姓名" width="100" />
        <el-table-column prop="patientPhone" label="电话" width="120" />
        <el-table-column label="医院" width="120"><template #default="{row}">{{ hospitalMap[row.hospitalId] || '-' }}</template></el-table-column>
        <el-table-column prop="doctorName" label="医师" width="100" />
        <el-table-column prop="repetition" label="付数" width="60" />
        <el-table-column label="配送" width="80"><template #default="{row}">{{ DELIVERY_TYPES.find(d => d.value === row.deliveryType)?.label || '-' }}</template></el-table-column>
        <el-table-column label="制剂" width="100"><template #default="{row}">{{ PREPARATION_TYPES.find(p => p.value === row.preparationType)?.label || '-' }}</template></el-table-column>
        <el-table-column label="服用" width="100"><template #default="{row}">{{ USAGE_METHODS.find(u => u.value === row.usageMethod)?.label || '-' }}</template></el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{row}"><el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="创建时间" width="160"><template #default="{row}">{{ formatTime(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openCreateDialog(row)">编辑</el-button>
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="row.receiveStatus === 'PENDING'" size="small" type="danger" @click="openRejectDialog(row)">驳回</el-button>
            <el-button v-if="row.receiveStatus === 'PENDING'" size="small" type="warning" @click="openUrgentDialog(row)">加急</el-button>
            <el-button v-if="row.receiveStatus === 'REJECTED'" size="small" type="success" @click="handleReactivate(row)">重新激活</el-button>
            <el-button v-if="row.receiveStatus === 'PENDING'" size="small" @click="handleCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 异常处方列表 -->
      <el-table v-else :data="exceptionList" v-loading="exceptionLoading" border>
        <el-table-column prop="prescriptionNumber" label="处方号" width="140" />
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
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="处方号" prop="prescriptionNumber">
              <el-input v-model="form.prescriptionNumber" placeholder="处方编号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="患者姓名" prop="patientName">
              <el-input v-model="form.patientName" placeholder="必填" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="患者电话">
              <el-input v-model="form.patientPhone" placeholder="联系电话" />
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
                <el-radio :value="0">门诊</el-radio>
                <el-radio :value="1">住院</el-radio>
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
          <el-col :span="8">
            <el-form-item label="制剂类型">
              <el-select v-model="form.preparationType" placeholder="请选择" clearable style="width:100%">
                <el-option v-for="p in PREPARATION_TYPES" :key="p.value" :label="p.label" :value="p.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="服用方法">
              <el-select v-model="form.usageMethod" placeholder="请选择" clearable style="width:100%">
                <el-option v-for="u in USAGE_METHODS" :key="u.value" :label="u.label" :value="u.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="配送方式">
              <el-select v-model="form.deliveryType" placeholder="请选择" clearable style="width:100%">
                <el-option v-for="d in DELIVERY_TYPES" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="配送地址">
              <el-input v-model="form.deliveryAddress" placeholder="配送地址" />
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
          <template #empty>
            <el-empty description="暂无药材，请点击「+ 添加药材」按钮添加" :image-size="60" />
          </template>
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
        <el-button type="primary" @click="handleSave">保存</el-button>
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
          <el-descriptions-item label="处方号">{{ detail.prescriptionNumber || detail.id }}</el-descriptions-item>
          <el-descriptions-item label="患者">{{ detail.patientName }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ detail.patientPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="医院">{{ hospitalMap[detail.hospitalId] || '-' }}</el-descriptions-item>
          <el-descriptions-item label="医师">{{ detail.doctorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ detail.patientType === 1 ? '住院' : '门诊' }}</el-descriptions-item>
          <el-descriptions-item label="付数">{{ detail.repetition }}</el-descriptions-item>
          <el-descriptions-item label="制剂">{{ PREPARATION_TYPES.find(p => p.value === detail.preparationType)?.label || '-' }}</el-descriptions-item>
          <el-descriptions-item label="服用">{{ USAGE_METHODS.find(u => u.value === detail.usageMethod)?.label || '-' }}</el-descriptions-item>
          <el-descriptions-item label="配送">{{ DELIVERY_TYPES.find(d => d.value === detail.deliveryType)?.label || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ detail.deliveryAddress || '-' }}</el-descriptions-item>
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

    <!-- 驳回对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="驳回处方" width="400px">
      <el-form label-width="100px">
        <el-form-item label="驳回类型" required>
          <el-select v-model="rejectForm.rejectType" placeholder="请选择" style="width:100%">
            <el-option v-for="t in REJECT_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="驳回原因" required>
          <el-input v-model="rejectForm.reason" type="textarea" rows="3" placeholder="请输入驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- 加急对话框 -->
    <el-dialog v-model="urgentDialogVisible" title="加急处理" width="500px">
      <el-form label-width="100px">
        <el-form-item label="急诊级别" required>
          <el-radio-group v-model="urgentForm.level">
            <el-radio-button v-for="l in URGENT_LEVELS" :key="l.value" :label="l.value">{{ l.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="送达方式">
          <el-select v-model="urgentForm.deliveryType" placeholder="请选择" clearable style="width: 100%">
            <el-option label="自取" value="SELF_PICKUP" />
            <el-option label="配送" value="DELIVERY" />
            <el-option label="院内配送" value="IN_HOUSE_DELIVERY" />
          </el-select>
        </el-form-item>
        <el-form-item label="送达位置">
          <el-input v-model="urgentForm.deliveryLocation" placeholder="请输入送达位置（如：内科病房 302床）" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="urgentForm.delayReason" type="textarea" :rows="2" placeholder="请输入急诊备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="urgentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUrgent">确认加急</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import request from '@/api/request'
import { useUserStore } from '@/stores/user'
import {
  listPrescriptions, createStructuredPrescription, updatePrescription,
  searchMedicines, importPrescriptionCsv,
  createPrescriptionFromOcr, uploadOcrImage,
  getPrescriptionDetail, rejectPrescription, markEmergency,
  reactivatePrescription, cancelPrescription
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
  prescriptionNumber: '',
  patientName: '',
  patientPhone: '',
  status: '',
  dateRange: null as [string, string] | null
})

// 主从数据
const schemes = ref<any[]>([])
const hospitals = ref<any[]>([])
const hospitalMap = ref<Record<number, string>>({})
const schemeMap = ref<Record<number, string>>({})

// 枚举定义
const DELIVERY_TYPES = [
  { label: '自取', value: 'SELF_PICKUP' },
  { label: '配送', value: 'DELIVERY' },
  { label: '院内配送', value: 'IN_HOUSE_DELIVERY' }
]
const PREPARATION_TYPES = [
  { label: '汤剂', value: 'DECOCTION' },
  { label: '浓煎剂', value: 'CONCENTRATED_DECOCTION' },
  { label: '普通散剂', value: 'COARSE_POWDER' },
  { label: '细粉', value: 'FINE_POWDER' },
  { label: '破壁粉', value: 'CELL_BROKEN_POWDER' },
  { label: '水丸', value: 'WATER_PILL' },
  { label: '蜜丸', value: 'HONEY_PILL' },
  { label: '浓缩丸', value: 'CONCENTRATED_PILL' },
  { label: '糊丸', value: 'PASTE_PILL' },
  { label: '膏方', value: 'MEDICINAL_PASTE' },
  { label: '配方颗粒', value: 'GRANULES' },
  { label: '酒剂（内服）', value: 'TINCTURE_INTERNAL' },
  { label: '酒剂（外用）', value: 'TINCTURE_EXTERNAL' },
  { label: '酊剂（内服）', value: 'SPIRIT_INTERNAL' },
  { label: '酊剂（外用）', value: 'SPIRIT_EXTERNAL' },
  { label: '茶剂', value: 'MEDICINAL_TEA' },
  { label: '丹剂', value: 'DAN_MEDICINE' },
  { label: '硬胶囊', value: 'HARD_CAPSULE' },
  { label: '软胶囊', value: 'SOFT_CAPSULE' },
  { label: '片剂', value: 'TABLET' },
  { label: '糖浆剂', value: 'SYRUP' },
  { label: '露剂', value: 'AROMATIC_WATER' },
  { label: '栓剂', value: 'SUPPOSITORY' },
  { label: '洗剂', value: 'WASH_SOLUTION' },
  { label: '其他', value: 'OTHER' }
]
const USAGE_METHODS = [
  { label: '内服', value: 'ORAL_INTERNAL' },
  { label: '外用', value: 'TOPICAL' },
  { label: '泡酒', value: 'WINE_SOAK' },
  { label: '熏蒸', value: 'FUMIGATION' },
  { label: '代茶饮', value: 'HERBAL_TEA' },
  { label: '水煎服', value: 'WATER_DECOCTION' },
  { label: '口服', value: 'ORAL' },
  { label: '温服', value: 'WARM_TAKE' },
  { label: '泡水代茶饮', value: 'INFUSION' },
  { label: '擦洗', value: 'WIPE_WASH' },
  { label: '敷贴', value: 'POULTICE' },
  { label: '浸泡', value: 'SOAK' },
  { label: '涂抹', value: 'APPLY' },
  { label: '煎服', value: 'DECOCT_TAKE' },
  { label: '冲服', value: 'DISSOLVE_TAKE' },
  { label: '灌肠', value: 'ENEMA' },
  { label: '含漱', value: 'GARGLE' },
  { label: '酊剂外用', value: 'TINCTURE_APPLY' },
  { label: '喷雾', value: 'SPRAY' },
  { label: '撒粉', value: 'DUSTING' },
  { label: '滴眼', value: 'EYE_DROPS' },
  { label: '滴耳', value: 'EAR_DROPS' },
  { label: '炖服', value: 'STEW_TAKE' },
  { label: '熏洗', value: 'STEAM_WASH' },
  { label: '涂擦', value: 'RUB' },
  { label: '输液', value: 'INFUSION_IV' }
]

// 创建对话框
const createDialogVisible = ref(false)
const form = reactive<any>({
  prescriptionNumber: '', patientName: '', patientPhone: '',
  hospitalId: null, patientType: 0,
  doctorName: '', schemeId: null, repetition: 7,
  deliveryType: '', deliveryAddress: '',
  preparationType: '', usageMethod: '',
  remark: '', medicineItems: [] as MedicineItem[]
})

const formRef = ref<any>(null)

const formRules = {
  prescriptionNumber: [{ required: true, message: '请输入处方号', trigger: 'blur' }],
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

// 驳回
const rejectDialogVisible = ref(false)
const rejectForm = reactive({ id: null as number | null, rejectType: '', reason: '' })
const REJECT_TYPES = [
  { label: '审方未过', value: 'REVIEW_REJECTED' },
  { label: '调剂复核未过', value: 'DISPENSING_REJECTED' },
  { label: '其他', value: 'OTHER' }
]

// 加急
const urgentDialogVisible = ref(false)
const urgentForm = reactive({
  id: null as number | null,
  level: 1,
  deliveryType: '',
  deliveryLocation: '',
  delayReason: ''
})
const URGENT_LEVELS = [
  { label: '普通急诊', value: 1 },
  { label: '危重急诊', value: 2 },
  { label: '抢救', value: 3 }
]

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
    if (search.prescriptionNumber) params.prescriptionNumber = search.prescriptionNumber
    if (search.patientName) params.patientName = search.patientName
    if (search.patientPhone) params.patientPhone = search.patientPhone
    if (search.status) params.status = search.status
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
  search.prescriptionNumber = ''; search.patientName = ''; search.patientPhone = ''
  search.status = ''; search.dateRange = null
  page.value = 1; fetchData()
}

function statusType(s?: string) {
  const map: Record<string, string> = { PENDING: 'info', PROCESSING: 'warning', COMPLETED: 'success' }
  return map[s || ''] || ''
}

function statusLabel(s?: string) {
  const map: Record<string, string> = { PENDING: '待处理', PROCESSING: '处理中', COMPLETED: '处理完毕' }
  return map[s || ''] || s || '-'
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

async function openCreateDialog(row?: any) {
  if (row) {
    // 编辑模式：先加载详情（列表数据不含药材明细）
    if (!row.id) {
      ElMessage.error('处方ID无效，无法编辑')
      return
    }
    const detailRes: any = await getPrescriptionDetail(row.id)
    const detail = detailRes.data
    form.id = detail.id
    form.prescriptionNumber = detail.prescriptionNumber || ''
    form.patientName = detail.patientName || ''
    form.patientPhone = detail.patientPhone || ''
    form.hospitalId = detail.hospitalId || null
    form.patientType = detail.patientType ?? 0
    form.doctorName = detail.doctorName || ''
    form.schemeId = detail.schemeId || null
    form.repetition = detail.repetition || 7
    form.deliveryType = detail.deliveryType || ''
    form.deliveryAddress = detail.deliveryAddress || ''
    form.preparationType = detail.preparationType || ''
    form.usageMethod = detail.usageMethod || ''
    form.remark = detail.remark || ''
    form.medicineItems = detail.medicineItems?.map((m: any) => ({
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
    form.prescriptionNumber = ''; form.patientName = ''; form.patientPhone = ''
    form.hospitalId = null; form.patientType = 0
    form.doctorName = ''; form.schemeId = null; form.repetition = 7
    form.deliveryType = ''; form.deliveryAddress = ''
    form.preparationType = ''; form.usageMethod = ''
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
let medicineQueryTimer: ReturnType<typeof setTimeout> | null = null

function queryMedicines(query: string, cb: (results: any[]) => void) {
  if (!query || query.length < 2) { cb([]); return }
  if (medicineQueryTimer) clearTimeout(medicineQueryTimer)
  medicineQueryTimer = setTimeout(async () => {
    try {
      const res: any = await searchMedicines(query)
      cb((res.data?.records || []).map((m: any) => ({ ...m, value: m.medicineName })))
    } catch { cb([]) }
  }, 300)
}

function onMedicineSelect(selected: any, row: any) {
  row.medicineId = selected.id
  row.medicineName = selected.medicineName
  row.unit = selected.unit || 'g'
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    // 前端防御：确保药材明细有效
    const validItems = (form.medicineItems || []).filter((item: MedicineItem) =>
      item && item.medicineName && item.medicineName.trim() && item.dosage != null && item.dosage > 0
    )
    if (validItems.length === 0) {
      ElMessage.warning('请至少添加一条有效的药材明细（名称和用量必填）')
      return
    }
    const payload = {
      prescriptionNumber: form.prescriptionNumber || '',
      patientName: form.patientName || '',
      patientPhone: form.patientPhone || '',
      hospitalId: form.hospitalId,
      patientType: Number(form.patientType) || 0,
      doctorName: form.doctorName || '',
      schemeId: form.schemeId || null,
      repetition: Number(form.repetition) || 7,
      deliveryType: form.deliveryType || '',
      deliveryAddress: form.deliveryAddress || '',
      preparationType: form.preparationType || '',
      usageMethod: form.usageMethod || '',
      remark: form.remark || '',
      source: 'MANUAL',
      medicineItems: validItems.map((item: MedicineItem, idx: number) => ({
        medicineId: item.medicineId || null,
        medicineName: item.medicineName.trim(),
        dosage: Number(item.dosage),
        unit: item.unit || 'g',
        decoctionMethod: item.decoctionMethod || 'NORMAL',
        sortOrder: idx + 1,
        batchNo: item.batchNo || null
      }))
    }
    console.log('SAVE PAYLOAD:', JSON.stringify(payload))
    if (form.id) {
      await updatePrescription(form.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createStructuredPrescription(payload)
      ElMessage.success('创建成功')
    }
    createDialogVisible.value = false
    fetchData()
  } catch (err: any) {
    console.error('SAVE ERROR:', err)
    ElMessage.error(err?.message || '保存失败')
  }
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
  if (!row.id) {
    ElMessage.error('处方ID无效，无法纠正')
    return
  }
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
  if (!row.id) {
    ElMessage.error('处方ID无效，无法查看详情')
    return
  }
  detailVisible.value = true
  detailLoading.value = true
  showRawData.value = false
  try {
    const res: any = await getPrescriptionDetail(row.id)
    detail.value = res.data || null
  } catch (e: any) {
    detail.value = null
    ElMessage.error(e?.response?.data?.message || '详情加载失败')
  } finally { detailLoading.value = false }
}

// ==================== 驳回 ====================

function openRejectDialog(row: any) {
  rejectForm.id = row.id
  rejectForm.rejectType = ''
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

async function handleReject() {
  if (!rejectForm.rejectType) { ElMessage.warning('请选择驳回类型'); return }
  if (!rejectForm.reason) { ElMessage.warning('请输入驳回原因'); return }
  const userStore = useUserStore()
  try {
    await rejectPrescription(rejectForm.id!, {
      rejectType: rejectForm.rejectType,
      reason: rejectForm.reason,
      operatorId: userStore.userInfo?.id || 0,
      operatorName: userStore.userInfo?.username || 'admin'
    })
    ElMessage.success('驳回成功')
    rejectDialogVisible.value = false
    fetchData()
  } catch { /* handled */ }
}

// ==================== 加急 ====================

function openUrgentDialog(row: any) {
  urgentForm.id = row.id
  urgentForm.level = 1
  urgentForm.deliveryType = ''
  urgentForm.deliveryLocation = ''
  urgentForm.delayReason = ''
  urgentDialogVisible.value = true
}

async function handleUrgent() {
  try {
    await markEmergency(urgentForm.id!, urgentForm.level, {
      deliveryType: urgentForm.deliveryType || undefined,
      deliveryLocation: urgentForm.deliveryLocation || undefined,
      delayReason: urgentForm.delayReason || undefined
    })
    ElMessage.success('加急成功')
    urgentDialogVisible.value = false
    fetchData()
  } catch {
    ElMessage.error('加急失败')
  }
}

async function handleReactivate(row: any) {
  try {
    await ElMessageBox.confirm(`确认重新激活处方 #${row.prescriptionNumber || row.id}？`, '重新激活', { type: 'warning' })
    await reactivatePrescription(row.id)
    ElMessage.success('重新激活成功')
    fetchData()
  } catch { /* cancel */ }
}

async function handleCancel(row: any) {
  try {
    await ElMessageBox.confirm(`确认取消处方 #${row.prescriptionNumber || row.id}？取消后将不可恢复。`, '取消处方', { type: 'warning' })
    await cancelPrescription(row.id)
    ElMessage.success('取消成功')
    fetchData()
  } catch { /* cancel */ }
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
    const enabled = list.filter((h: any) => h.status === 1 || h.status === 'ACTIVE')
    hospitals.value = enabled
    hospitalMap.value = Object.fromEntries(enabled.map((h: any) => [h.id, h.name]))
  } catch (e: any) {
    ElMessage.error('医院列表加载失败')
  }
}

onMounted(() => { fetchData(); fetchExceptions(); fetchSchemes(); fetchHospitals() })
</script>
