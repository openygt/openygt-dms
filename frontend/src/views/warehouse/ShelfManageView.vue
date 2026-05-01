<template>
  <div class="page-container">
    <div class="page-header-title">成品暂存：<span class="page-header-sub">成品货架、入库/出库、库存预警</span></div>
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline @submit.prevent>
        <el-form-item label="区域">
          <el-select v-model="searchForm.zone" clearable placeholder="全部" style="width: 140px" @change="handleSearch">
            <el-option label="A区" value="A" />
            <el-option label="B区" value="B" />
            <el-option label="C区" value="C" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" clearable placeholder="全部" style="width: 120px" @change="handleSearch">
            <el-option label="空闲" value="FREE" />
            <el-option label="使用中" value="IN_USE" />
            <el-option label="已满" value="FULL" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="货架编码">
          <el-input v-model="searchForm.code" clearable placeholder="货架编码" style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 货架地图 -->
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>货架地图</span>
          <div style="display: flex; gap: 12px; align-items: center">
            <span class="legend-dot free"></span> 空闲
            <span class="legend-dot in-use"></span> 使用中
            <span class="legend-dot full"></span> 已满
            <span class="legend-dot disabled"></span> 禁用
          </div>
        </div>
      </template>
      <div class="shelf-map">
        <div v-for="zone in shelfZones" :key="zone.name" class="zone-block">
          <div class="zone-title">{{ zone.name }}</div>
          <div class="zone-grid">
            <div
              v-for="shelf in zone.shelves"
              :key="shelf.code"
              class="shelf-cell"
              :class="shelf.status.toLowerCase().replace('_', '-')"
              @click="selectShelf(shelf)"
            >
              <div class="cell-code">{{ shelf.code }}</div>
              <div class="cell-capacity">{{ shelf.current }}/{{ shelf.capacity }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 货架列表 -->
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>货架列表</span>
          <div style="display: flex; gap: 8px">
            <el-button type="primary" @click="openPutOnDialog">上架</el-button>
            <el-button type="warning" @click="openTakeOffDialog">下架</el-button>
          </div>
        </div>
      </template>
      <el-table :data="tableData" v-loading="loading" border @row-click="selectShelfRow">
        <el-table-column prop="code" label="货架编码" min-width="120" />
        <el-table-column prop="zone" label="区域" width="80" />
        <el-table-column prop="rowNo" label="排号" width="80" />
        <el-table-column prop="layer" label="层号" width="80" />
        <el-table-column prop="capacity" label="容量" width="90" />
        <el-table-column prop="current" label="当前数量" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" min-width="160" />
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="handleSearch"
        />
      </div>
    </el-card>

    <!-- 上架弹窗 -->
    <el-dialog v-model="putOnDialogVisible" title="上架操作" width="520px">
      <el-form :model="putOnForm" label-width="100px">
        <el-form-item label="药袋条码" required>
          <el-input v-model="putOnForm.bagCode" placeholder="扫描药袋条码">
            <template #append>
              <el-button @click="simulateScan">模拟扫码</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="处方号">
          <el-input v-model="putOnForm.prescriptionNo" disabled />
        </el-form-item>
        <el-form-item label="患者姓名">
          <el-input v-model="putOnForm.patientName" disabled />
        </el-form-item>
        <el-form-item label="目标货架" required>
          <el-select v-model="putOnForm.shelfCode" placeholder="选择货架" style="width: 100%">
            <el-option
              v-for="s in availableShelves"
              :key="s.code"
              :label="`${s.code} (${s.zone}区 ${s.current}/${s.capacity})`"
              :value="s.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="putOnForm.remark" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="putOnDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePutOn">确认上架</el-button>
      </template>
    </el-dialog>

    <!-- 下架弹窗 -->
    <el-dialog v-model="takeOffDialogVisible" title="下架操作" width="520px">
      <el-form :model="takeOffForm" label-width="100px">
        <el-form-item label="药袋条码" required>
          <el-input v-model="takeOffForm.bagCode" placeholder="扫描药袋条码">
            <template #append>
              <el-button @click="simulateScanOff">模拟扫码</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="处方号">
          <el-input v-model="takeOffForm.prescriptionNo" disabled />
        </el-form-item>
        <el-form-item label="当前货架">
          <el-input v-model="takeOffForm.shelfCode" disabled />
        </el-form-item>
        <el-form-item label="下架原因">
          <el-radio-group v-model="takeOffForm.reason">
            <el-radio label="PICKUP">患者取药</el-radio>
            <el-radio label="REWORK">返工</el-radio>
            <el-radio label="TRANSFER">转库</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="takeOffForm.remark" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="takeOffDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="handleTakeOff">确认下架</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getShelfList, putOnShelf, takeOffShelf } from '@/api/newModules'

const loading = ref(false)
const searchForm = reactive({ zone: '', status: '', code: '' })
const tableData = ref<any[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })

const statusTagType = (status: string) => {
  if (status === 'FREE') return 'success'
  if (status === 'IN_USE') return 'primary'
  if (status === 'FULL') return 'danger'
  if (status === 'DISABLED') return 'info'
  return ''
}
const statusText = (status: string) => {
  if (status === 'FREE') return '空闲'
  if (status === 'IN_USE') return '使用中'
  if (status === 'FULL') return '已满'
  if (status === 'DISABLED') return '禁用'
  return status
}

async function handleSearch() {
  loading.value = true
  try {
    const res = await getShelfList({ ...searchForm, page: pagination.page, size: pagination.size }) as any
    tableData.value = res.data?.list || []
    pagination.total = res.data?.total || 0
  } catch {
    tableData.value = [
      { code: 'A-01-01', zone: 'A', rowNo: '01', layer: '01', capacity: 20, current: 12, status: 'IN_USE', updateTime: '2024-05-01 10:00' },
      { code: 'A-01-02', zone: 'A', rowNo: '01', layer: '02', capacity: 20, current: 20, status: 'FULL', updateTime: '2024-05-01 09:30' },
      { code: 'A-02-01', zone: 'A', rowNo: '02', layer: '01', capacity: 20, current: 0, status: 'FREE', updateTime: '2024-05-01 08:00' },
      { code: 'B-01-01', zone: 'B', rowNo: '01', layer: '01', capacity: 15, current: 5, status: 'IN_USE', updateTime: '2024-05-01 11:00' },
      { code: 'B-01-02', zone: 'B', rowNo: '01', layer: '02', capacity: 15, current: 0, status: 'FREE', updateTime: '2024-05-01 08:00' },
      { code: 'C-01-01', zone: 'C', rowNo: '01', layer: '01', capacity: 10, current: 0, status: 'DISABLED', updateTime: '2024-04-30 18:00' }
    ]
    pagination.total = 6
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  searchForm.zone = ''
  searchForm.status = ''
  searchForm.code = ''
  pagination.page = 1
  handleSearch()
}

const shelfZones = computed(() => {
  const zones: Record<string, any[]> = {}
  tableData.value.forEach(s => {
    if (!zones[s.zone]) zones[s.zone] = []
    zones[s.zone].push(s)
  })
  return Object.keys(zones).map(name => ({ name: `${name}区`, shelves: zones[name] }))
})

function selectShelf(shelf: any) {
  ElMessage.info(`选中货架：${shelf.code}`)
}

function selectShelfRow(row: any) {
  ElMessage.info(`选中货架：${row.code}`)
}

const availableShelves = computed(() => tableData.value.filter(s => s.status !== 'FULL' && s.status !== 'DISABLED'))

const putOnDialogVisible = ref(false)
const putOnForm = reactive({ bagCode: '', prescriptionNo: '', patientName: '', shelfCode: '', remark: '' })

function openPutOnDialog() {
  putOnDialogVisible.value = true
  putOnForm.bagCode = ''
  putOnForm.prescriptionNo = ''
  putOnForm.patientName = ''
  putOnForm.shelfCode = ''
  putOnForm.remark = ''
}

function simulateScan() {
  putOnForm.bagCode = 'BG20240501001'
  putOnForm.prescriptionNo = 'P202405010001'
  putOnForm.patientName = '王患者'
  ElMessage.success('扫码成功')
}

async function handlePutOn() {
  if (!putOnForm.bagCode) { ElMessage.warning('请扫描药袋条码'); return }
  if (!putOnForm.shelfCode) { ElMessage.warning('请选择目标货架'); return }
  try {
    await putOnShelf(putOnForm)
    ElMessage.success('上架成功')
    putOnDialogVisible.value = false
    handleSearch()
  } catch {
    ElMessage.error('上架失败')
  }
}

const takeOffDialogVisible = ref(false)
const takeOffForm = reactive({ bagCode: '', prescriptionNo: '', shelfCode: '', reason: 'PICKUP', remark: '' })

function openTakeOffDialog() {
  takeOffDialogVisible.value = true
  takeOffForm.bagCode = ''
  takeOffForm.prescriptionNo = ''
  takeOffForm.shelfCode = ''
  takeOffForm.reason = 'PICKUP'
  takeOffForm.remark = ''
}

function simulateScanOff() {
  takeOffForm.bagCode = 'BG20240501001'
  takeOffForm.prescriptionNo = 'P202405010001'
  takeOffForm.shelfCode = 'A-01-01'
  ElMessage.success('扫码成功')
}

async function handleTakeOff() {
  if (!takeOffForm.bagCode) { ElMessage.warning('请扫描药袋条码'); return }
  try {
    await takeOffShelf(takeOffForm)
    ElMessage.success('下架成功')
    takeOffDialogVisible.value = false
    handleSearch()
  } catch {
    ElMessage.error('下架失败')
  }
}

onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.search-card {
  margin-top: 8px;
}
.shelf-map {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.zone-block {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--el-border-radius-base);
  padding: 12px;
}
.zone-title {
  font-weight: 600;
  margin-bottom: 10px;
  color: var(--el-text-color-primary);
}
.zone-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 10px;
}
.shelf-cell {
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  padding: 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}
.shelf-cell:hover {
  transform: translateY(-2px);
  box-shadow: var(--el-box-shadow-lighter);
}
.shelf-cell.free { background: #f0f9eb; border-color: #b3e19d; }
.shelf-cell.in-use { background: #ecf5ff; border-color: #a0cfff; }
.shelf-cell.full { background: #fef0f0; border-color: #fab6b6; }
.shelf-cell.disabled { background: #f4f4f5; border-color: #c8c9cc; color: #a8abb2; }
.cell-code { font-weight: 600; font-size: 13px; }
.cell-capacity { font-size: 12px; margin-top: 4px; color: var(--el-text-color-secondary); }
.legend-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 4px;
}
.legend-dot.free { background: #b3e19d; }
.legend-dot.in-use { background: #a0cfff; }
.legend-dot.full { background: #fab6b6; }
.legend-dot.disabled { background: #c8c9cc; }
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
