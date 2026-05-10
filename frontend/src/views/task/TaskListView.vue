<template>
  <div>
    <div class="page-header-title">
      {{ pageTitle }}<span v-if="pageDesc" class="page-header-sub">：{{ pageDesc }}</span>
    </div>
    <div style="height: 16px"></div>
    <el-card>
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button label="list">列表视图</el-radio-button>
            <el-radio-button label="kanban">看板视图</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="任务号">
          <el-input v-model="queryForm.id" placeholder="任务号" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="处方号">
          <el-input v-model="queryForm.prescriptionNumber" placeholder="处方号" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option v-for="item in kanbanStatuses" :key="item.status" :label="item.label" :value="item.status" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input
            v-model="queryForm.operatorKeyword"
            placeholder="姓名或用户ID"
            clearable
            style="width: 140px"
            :disabled="onlyMine"
          />
        </el-form-item>
        <el-form-item label="">
          <el-checkbox v-model="onlyMine" :disabled="!hasTaskManage">仅我的任务</el-checkbox>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <template v-if="viewMode === 'list'">
        <el-table :data="taskList" v-loading="loading" border style="width: 100%">
          <el-table-column prop="id" label="任务号" width="80" />
          <el-table-column label="处方号" width="200">
            <template #default="{ row }">
              {{ formatPrescriptionNo(row) }}
            </template>
          </el-table-column>
          <el-table-column prop="statusLabel" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" effect="dark" disable-transitions>{{ row.statusLabel || row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作人" width="100">
            <template #default="{ row }">
              {{ row.operatorName || row.operatorId || '-' }}
            </template>
          </el-table-column>
          <el-table-column width="78" align="center">
            <template #header>
              <el-tooltip
                content="由质检不通过、报废、设备告警等业务将任务标为异常（is_exception）；详情见异常原因"
                placement="top"
              >
                <span class="col-hint">异常</span>
              </el-tooltip>
            </template>
            <template #default="{ row }">
              <el-tag v-if="row.isException" type="danger" size="small">是</el-tag>
              <span v-else style="color: var(--el-text-color-placeholder)">否</span>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="360" fixed="right">
            <template #default="{ row }">
              <el-button v-if="hasTaskManage && row.status === 'WAIT_SOAK'" size="small" type="primary" @click="startSoak(row)">开始泡药</el-button>
              <el-button v-if="hasTaskManage && row.status === 'WAIT_DECOCT'" size="small" type="warning" @click="openDecoctDialog(row)">开始煎药</el-button>
              <el-button size="small" @click="viewDetail(row)">详情</el-button>
              <el-dropdown trigger="click" @command="(c: string) => handleRowCommand(c, row)">
                <el-button size="small">
                  更多
                  <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="trace">流程跟踪</el-dropdown-item>
                    <el-dropdown-item v-if="hasTaskManage" command="herb">分组投料</el-dropdown-item>
                    <el-dropdown-item v-if="hasTaskManage && canSuspend(row)" command="suspend">挂起</el-dropdown-item>
                    <el-dropdown-item v-if="hasTaskManage && row.status === 'SUSPENDED'" command="resume">恢复</el-dropdown-item>
                    <el-dropdown-item v-if="hasTaskManage" command="assign">改派操作人</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
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
      </template>

      <template v-else>
        <div v-loading="loading" class="kanban-board">
          <div v-for="col in kanbanColumns" :key="col.status" class="kanban-column">
            <div class="kanban-header">
              <span class="kanban-title">{{ col.label }}</span>
              <el-tag size="small" :type="statusType(col.status)">{{ col.tasks.length }}</el-tag>
            </div>
            <div class="kanban-body">
              <div v-for="task in col.tasks" :key="task.id" class="kanban-card" @click="viewDetail(task)">
                <div class="kanban-card-top">
                  <span class="kanban-card-id">#{{ task.id }}</span>
                  <el-tag size="small" :type="statusType(task.status)">{{ task.statusLabel || task.status }}</el-tag>
                </div>
                <div class="kanban-card-info">处方: {{ formatPrescriptionNo(task) }}</div>
                <div class="kanban-card-info">操作人: {{ task.operatorName || task.operatorId || '-' }}</div>
                <div class="kanban-card-time">{{ formatDateTime(task.updatedAt) }}</div>
                <div class="kanban-card-actions">
                  <el-button v-if="hasTaskManage && task.status === 'WAIT_SOAK'" size="small" type="primary" @click.stop="startSoak(task)">开始泡药</el-button>
                  <el-button v-if="hasTaskManage && task.status === 'WAIT_DECOCT'" size="small" type="warning" @click.stop="openDecoctDialog(task)">开始煎药</el-button>
                  <el-dropdown trigger="click" @command="(c: string) => handleRowCommand(c, task)">
                    <el-button size="small" @click.stop>更多</el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="trace">流程跟踪</el-dropdown-item>
                        <el-dropdown-item v-if="hasTaskManage" command="herb">分组投料</el-dropdown-item>
                        <el-dropdown-item v-if="hasTaskManage && canSuspend(task)" command="suspend">挂起</el-dropdown-item>
                        <el-dropdown-item v-if="hasTaskManage && task.status === 'SUSPENDED'" command="resume">恢复</el-dropdown-item>
                        <el-dropdown-item v-if="hasTaskManage" command="assign">改派</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>
              <el-empty v-if="col.tasks.length === 0" description="无任务" :image-size="60" />
            </div>
          </div>
        </div>
      </template>
    </el-card>

    <el-dialog v-model="detailVisible" title="任务详情" width="700px">
      <template v-if="selectedTask">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="任务号" span="1">{{ selectedTask.id }}</el-descriptions-item>
          <el-descriptions-item label="条码" span="1">{{ selectedTask.barcode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态" span="1">
            <el-tag :type="statusType(selectedTask.status)" size="small">{{ selectedTask.statusLabel || selectedTask.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="处方号" span="2">{{ formatPrescriptionNo(selectedTask) }}</el-descriptions-item>
          <el-descriptions-item label="操作人" span="1">{{ selectedTask.operatorName || selectedTask.operatorId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="煎药设备" span="1">{{ selectedTask.decoctDeviceId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="包装设备" span="1">{{ selectedTask.packageDeviceId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前温度" span="1">{{ selectedTask.currentTemp != null ? selectedTask.currentTemp + '°C' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="目标温度" span="1">{{ selectedTask.targetTemp != null ? selectedTask.targetTemp + '°C' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="泡药时长" span="1">{{ selectedTask.soakDuration != null ? selectedTask.soakDuration + '分钟' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="异常" span="1">
            <el-tag v-if="selectedTask.isException" type="danger" size="small">是</el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedTask.exceptionReason" label="异常原因" span="2">{{ selectedTask.exceptionReason }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" span="1">{{ formatDateTime(selectedTask.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间" span="1">{{ formatDateTime(selectedTask.updatedAt) }}</el-descriptions-item>
          <el-descriptions-item label="泡药开始" span="1">{{ formatDateTime(selectedTask.soakStartTime) }}</el-descriptions-item>
          <el-descriptions-item label="泡药结束" span="1">{{ formatDateTime(selectedTask.soakEndTime) }}</el-descriptions-item>
          <el-descriptions-item label="煎药开始" span="1">{{ formatDateTime(selectedTask.decoctStartTime) }}</el-descriptions-item>
          <el-descriptions-item label="煎药结束" span="1">{{ formatDateTime(selectedTask.decoctEndTime) }}</el-descriptions-item>
          <el-descriptions-item label="出液开始" span="1">{{ formatDateTime(selectedTask.pourStartTime) }}</el-descriptions-item>
          <el-descriptions-item label="出液结束" span="1">{{ formatDateTime(selectedTask.pourEndTime) }}</el-descriptions-item>
          <el-descriptions-item label="包装开始" span="1">{{ formatDateTime(selectedTask.wrapStartTime) }}</el-descriptions-item>
          <el-descriptions-item label="包装结束" span="1">{{ formatDateTime(selectedTask.wrapEndTime) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间" span="1">{{ formatDateTime(selectedTask.completeTime) }}</el-descriptions-item>
          <el-descriptions-item label="交接类型" span="1">{{ selectedTask.handoverType || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button v-if="selectedTask" type="primary" link @click="goStepTrace(selectedTask)">流程跟踪</el-button>
        <el-button v-if="hasTaskManage && selectedTask" type="primary" link @click="goHerbGroup(selectedTask)">分组投料</el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="decoctDialogVisible" title="开始煎药" :width="hasTaskManage ? '520px' : '420px'">
      <el-form label-width="100px">
        <el-form-item label="任务号">
          <span>{{ currentDecoctTask?.id }}</span>
        </el-form-item>
        <el-form-item v-if="hasTaskManage" label="空闲设备组">
          <div v-if="decoctIdleGroupTags.length" class="decoct-group-tags">
            <el-tag v-for="g in decoctIdleGroupTags" :key="g.id" size="small" class="decoct-group-tag">
              {{ g.label }}
            </el-tag>
          </div>
          <span v-else class="text-muted">暂无状态为 IDLE（或空）的设备组</span>
        </el-form-item>

        <el-form-item label="煎药设备" required>
          <el-select
            v-model="decoctDeviceCode"
            filterable
            clearable
            :placeholder="decoctDeviceSelectPlaceholder"
            style="width: 100%"
          >
            <template v-if="decoctDeviceSelectGroups">
              <el-option-group v-for="grp in decoctDeviceSelectGroups" :key="grp.label" :label="grp.label">
                <el-option
                  v-for="device in grp.devices"
                  :key="device.id"
                  :label="device.label"
                  :value="device.code"
                />
              </el-option-group>
            </template>
            <template v-else>
              <el-option v-for="device in decoctDevices" :key="device.id" :label="device.label" :value="device.code" />
            </template>

          </el-select>
        </el-form-item>
        <el-form-item v-if="hasTaskManage">
          <div class="text-muted">
            优先从全局空闲煎药机中选择；若当前无空闲设备但需要联调，可直接输入测试设备编码提交。
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="decoctDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStartDecoct">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="suspendDialogVisible" title="挂起任务" width="440px" @closed="suspendReason = ''">
      <el-form label-width="88px">
        <el-form-item label="挂起原因" required>
          <el-input v-model="suspendReason" type="textarea" :rows="3" placeholder="请填写挂起原因" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="suspendDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="confirmSuspend">确认挂起</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDialogVisible" title="改派操作人" width="420px">
      <el-form label-width="100px">
        <el-form-item label="任务号">
          <span>{{ assignTargetTask?.id }}</span>
        </el-form-item>
        <el-form-item label="新操作人" required>
          <el-select v-model="assignUserId" filterable clearable placeholder="选择系统用户" style="width: 100%">
            <el-option
              v-for="u in userOptions"
              :key="u.id"
              :label="`${u.name}（ID:${u.id}）`"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useMenuDesc } from '@/composables/useMenuDesc'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import request from '@/api/request'
import { useUserStore } from '@/stores/user'
import { getDeviceGroupsAll, getDeviceList } from '@/api/equipment'
import { startDecoct } from '@/api/newModules'

interface Task {
  id: number
  barcode: string
  prescriptionId: number
  prescriptionNumber: string
  status: string
  statusLabel: string
  currentStep: string
  operatorId: string
  operatorName: string
  decoctDeviceId: number
  packageDeviceId: number
  currentTemp: number
  targetTemp: number
  soakDuration: number
  isException: number
  exceptionReason: string
  createdAt: string
  updatedAt: string
  soakStartTime: string
  soakEndTime: string
  decoctStartTime: string
  decoctEndTime: string
  pourStartTime: string
  pourEndTime: string
  wrapStartTime: string
  wrapEndTime: string
  completeTime: string
  handoverType: string
  handoverUser: string
  handoverTime: string
}

const router = useRouter()
const userStore = useUserStore()

/** 管理员/主任/班组长：默认看全部任务；操作工等：默认「仅我的任务」 */
function usesOnlyMineDefault(): boolean {
  const roles: string[] = userStore.userInfo?.roles || []
  const perms: string[] = userStore.permissions || []
  const fullAccessRoles = ['ROLE_ADMIN', 'ROLE_DIRECTOR', 'ROLE_LEADER']
  const byRole = roles.some((r) => fullAccessRoles.includes(r))
  const byPerm = fullAccessRoles.some((r) => perms.includes(r))
  const adminName = String(userStore.userInfo?.username || '').toLowerCase() === 'admin'
  return !(byRole || byPerm || adminName)
}

/** 任务管理权限：管理员/主任/班组长或显式 prod:task:manage（与设备 eq:device:view 无关） */
const hasTaskManage = computed(() => {
  const roles: string[] = userStore.userInfo?.roles || []
  const perms: string[] = userStore.permissions || []
  const fullAccessRoles = ['ROLE_ADMIN', 'ROLE_DIRECTOR', 'ROLE_LEADER']
  const byRole = roles.some((r) => fullAccessRoles.includes(r))
  const byPerm = fullAccessRoles.some((r) => perms.includes(r))
  const adminName = String(userStore.userInfo?.username || '').toLowerCase() === 'admin'
  if (byRole || byPerm || adminName) return true
  return perms.includes('prod:task:manage')
})

const { title, description } = useMenuDesc()
const pageTitle = computed(() => title.value)
const pageDesc = computed(() => description.value)

function isDeviceGroupIdleStatus(s: unknown): boolean {
  if (s == null || String(s).trim() === '') return true
  return String(s).trim().toUpperCase() === 'IDLE'
}

function isDeviceRecordIdle(item: any): boolean {
  const s = item?.status
  if (s == null || String(s).trim() === '') return false
  return String(s).trim().toLowerCase() === 'idle'
}

/** 兜底拉全量时再筛：未维护状态视为空闲；兼容中文「空闲」等现场写法 */
function isDeviceRecordIdleLoose(item: any): boolean {
  const raw = item?.status
  if (raw == null || String(raw).trim() === '') return true
  const t = String(raw).trim()
  if (/空闲|待机|待命/i.test(t)) return true
  return t.toLowerCase() === 'idle'
}

const taskList = ref<Task[]>([])
const loading = ref(false)
const detailVisible = ref(false)
const decoctDialogVisible = ref(false)
const suspendDialogVisible = ref(false)
const assignDialogVisible = ref(false)
const selectedTask = ref<Task | null>(null)
const currentDecoctTask = ref<Task | null>(null)
const suspendTargetTask = ref<Task | null>(null)
const assignTargetTask = ref<Task | null>(null)
const suspendReason = ref('')
const assignUserId = ref<number | null>(null)
const decoctDeviceCode = ref('')
const decoctDevices = ref<{ id: number; code: string; label: string; groupId?: number; groupName?: string }[]>([])
const decoctIdleGroupTags = ref<{ id: number; label: string }[]>([])

const userOptions = ref<{ id: number; name: string }[]>([])
const viewMode = ref<'list' | 'kanban'>('list')
const onlyMine = ref(usesOnlyMineDefault())

watch(
  hasTaskManage,
  (ok) => {
    if (!ok) onlyMine.value = true
  },
  { immediate: true }
)

const queryForm = ref({
  id: '',
  prescriptionNumber: '',
  status: '',
  operatorKeyword: '',
  dateRange: null as [string, string] | null
})

const pagination = ref({ page: 1, size: 100, total: 0 })

/** 与后端 TaskStatusTransition 可挂起状态对齐（含待贴标） */
const SUSPENDABLE_STATUSES = new Set([
  'WAIT_SOAK', 'SOAKING', 'WAIT_DECOCT', 'DECOCTING', 'WAIT_POUR', 'POURING',
  'WAIT_WRAP', 'WRAPPING', 'WAIT_LABEL', 'WAIT_QC', 'STORED', '待交接'
])

const kanbanStatuses = [
  { status: 'WAIT_SOAK', label: '待泡药' },
  { status: 'SOAKING', label: '泡药中' },
  { status: 'WAIT_DECOCT', label: '待煎药' },
  { status: 'DECOCTING', label: '煎药中' },
  { status: 'WAIT_POUR', label: '待出液' },
  { status: 'POURING', label: '出液中' },
  { status: 'WAIT_WRAP', label: '待包装' },
  { status: 'WRAPPING', label: '包装中' },
  { status: 'WAIT_LABEL', label: '待贴标' },
  { status: 'WAIT_QC', label: '待质检' },
  { status: 'WAIT_HANDOVER', label: '待交接' },
  { status: 'COMPLETED', label: '已完成' }
]

const kanbanColumns = computed(() => kanbanStatuses.map(col => ({ ...col, tasks: taskList.value.filter(t => t.status === col.status) })))

const decoctDeviceSelectPlaceholder = computed(() =>
  hasTaskManage.value ? '全局空闲煎药设备（按设备组分组）' : '仅显示空闲(IDLE)煎药设备'
)

/** 任务管理权限：下拉按设备组分组；否则扁平列表 */
const decoctDeviceSelectGroups = computed(() => {
  if (!hasTaskManage.value) return null
  const by = new Map<string, typeof decoctDevices.value>()
  for (const d of decoctDevices.value) {
    const k = d.groupName || '未分组'
    if (!by.has(k)) by.set(k, [])
    by.get(k)!.push(d)
  }
  return [...by.entries()]
    .sort((a, b) => a[0].localeCompare(b[0], 'zh-CN'))
    .map(([label, devices]) => ({
      label,
      devices: [...devices].sort((x, y) => (x.code || '').localeCompare(y.code || '', 'zh-CN'))
    }))
})


/** 避免处方号列显示成与任务号相同的处方ID，造成误解 */
function formatPrescriptionNo(row: Task) {
  const num = row.prescriptionNumber?.trim()
  if (num) return num
  const pid = row.prescriptionId
  if (pid == null) return '-'
  if (Number(pid) === Number(row.id)) return '—'
  return `内部#${pid}`
}

function canSuspend(row: Task) {
  return SUSPENDABLE_STATUSES.has(row.status)
}

function statusType(status: string) {
  const map: Record<string, string> = {
    'WAIT_SOAK': '', 'SOAKING': 'warning', 'WAIT_DECOCT': 'info', 'DECOCTING': 'danger',
    'WAIT_POUR': 'info', 'POURING': 'warning', 'WAIT_WRAP': 'info', 'WRAPPING': 'warning',
    'WAIT_LABEL': 'success', 'WAIT_QC': 'primary', 'WAIT_HANDOVER': 'success', 'COMPLETED': 'success',
    'SUSPENDED': 'info'
  }
  return map[status] || ''
}

function formatDateTime(dt: string) {
  if (!dt) return '-'
  const d = new Date(dt)
  if (Number.isNaN(d.getTime())) return dt
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function effectiveOperatorId(): string | undefined {
  if (!hasTaskManage.value) {
    const id = userStore.userInfo?.id
    return id != null ? String(id) : undefined
  }
  if (!onlyMine.value) return undefined
  const id = userStore.userInfo?.id
  return id != null ? String(id) : undefined
}

/** 开始煎药：只拉取空闲煎药设备；有任务管理权限时拉全量分页并展示设备组 */
async function fetchDecoctDevicesIdle() {
  decoctIdleGroupTags.value = []

  const pageSize = hasTaskManage.value ? 1000 : 200
  let res: any = await getDeviceList({ page: 1, size: pageSize, deviceType: 1, status: 'IDLE' })
  let records: any[] = res.data?.records || []
  // 兜底：首查为空时再拉煎药机全状态，在前端筛出 IDLE（避免库状态写法异常或接口过滤不一致）
  if (records.length === 0) {
    res = await getDeviceList({ page: 1, size: pageSize, deviceType: 1 })
    const all = res.data?.records || []
    records = all.filter(isDeviceRecordIdleLoose)
    if (records.length > 0) {
      ElMessage.info('已在全部煎药机中筛出空闲(IDLE)设备')
    }
  }
  // 仍无数据：可能 device_type 未标成 1，管理端再拉全类型设备中的空闲项
  if (records.length === 0 && hasTaskManage.value) {
    res = await getDeviceList({ page: 1, size: pageSize })
    const all = res.data?.records || []
    records = all.filter(isDeviceRecordIdleLoose)
    if (records.length > 0) {
      ElMessage.warning('未找到 device_type=1 的空闲煎药机，已列出全部类型中的空闲设备，请在设备管理核对类型')
    }
  }

  let groupMap: Record<number, string> = {}
  if (hasTaskManage.value) {
    try {
      const gres: any = await getDeviceGroupsAll()
      const groups: any[] = gres.data || []
      for (const g of groups) {
        if (g?.id != null) {
          groupMap[g.id] = String(g.groupName || g.groupCode || `组#${g.id}`)
        }
      }
      decoctIdleGroupTags.value = groups
        .filter((g) => isDeviceGroupIdleStatus(g?.status))
        .map((g) => ({
          id: g.id,
          label: `${g.groupName || g.groupCode || `组#${g.id}`}${g.groupCode && g.groupName ? ` (${g.groupCode})` : ''}`
        }))
    } catch {
      decoctIdleGroupTags.value = []
    }

  }

  decoctDevices.value = records.map((item: any) => {
    const gid = item.groupId as number | undefined
    const gname = gid != null ? groupMap[gid] : undefined
    const typeTag = item.deviceType != null && item.deviceType !== 1 ? `[类型${item.deviceType}] ` : ''
    const base = `${typeTag}${item.name || item.deviceCode} (${item.deviceCode})`

    const label = base
    return {
      id: item.id,
      code: item.deviceCode,
      label,
      groupId: gid,
      groupName: gname
    }
  })

  if (hasTaskManage.value) {
    decoctDevices.value.sort((a, b) => {
      const ga = a.groupName || '\uFFFF'
      const gb = b.groupName || '\uFFFF'
      if (ga !== gb) return ga.localeCompare(gb, 'zh-CN')

      return (a.code || '').localeCompare(b.code || '', 'zh-CN')
    })
  }
}

async function fetchUserOptions() {
  try {
    const res: any = await request.get('/v1/sys/users', { params: { page: 1, size: 500 } })
    userOptions.value = (res.data?.records || []).map((item: any) => ({
      id: item.id,
      name: item.realName || item.username || `用户-${item.id}`
    }))
  } catch {
    userOptions.value = []
  }
}

async function fetchTasks() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.value.page,
      size: viewMode.value === 'kanban' ? 500 : pagination.value.size
    }
    if (queryForm.value.id) params.id = queryForm.value.id
    if (queryForm.value.prescriptionNumber) params.prescriptionNumber = queryForm.value.prescriptionNumber
    if (queryForm.value.status) params.status = queryForm.value.status
    const op = effectiveOperatorId()
    if (op) params.operatorId = op
    else if (queryForm.value.operatorKeyword?.trim()) {
      params.operatorKeyword = queryForm.value.operatorKeyword.trim()
    }
    if (queryForm.value.dateRange?.[0]) {
      params.startTime = `${queryForm.value.dateRange[0]} 00:00:00`
      params.endTime = `${queryForm.value.dateRange[1]} 23:59:59`
    }
    const res: any = await request.get('/v1/prod/tasks', { params })
    taskList.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  pagination.value.page = 1
  fetchTasks()
}

function handleReset() {
  onlyMine.value = hasTaskManage.value ? usesOnlyMineDefault() : true
  queryForm.value = {
    id: '',
    prescriptionNumber: '',
    status: '',
    operatorKeyword: '',
    dateRange: null
  }
  pagination.value.page = 1
  fetchTasks()
}

function handleSizeChange(val: number) {
  pagination.value.size = val
  pagination.value.page = 1
  fetchTasks()
}

function handlePageChange(val: number) {
  pagination.value.page = val
  fetchTasks()
}

function goStepTrace(row: Task) {
  detailVisible.value = false
  router.push({ path: '/step-visualization', query: { taskId: String(row.id) } })
}

function goHerbGroup(row: Task) {
  if (!row.prescriptionId) {
    ElMessage.warning('该任务无处方ID，无法打开分组投料')
    return
  }
  detailVisible.value = false
  router.push({ path: '/herb-group', query: { prescriptionId: String(row.prescriptionId) } })
}

function handleRowCommand(cmd: string, row: Task) {
  if (!hasTaskManage.value && ['herb', 'suspend', 'resume', 'assign'].includes(cmd)) {
    ElMessage.warning('无任务管理权限，无法在 Web 端执行该操作')
    return
  }
  if (cmd === 'trace') goStepTrace(row)
  else if (cmd === 'herb') goHerbGroup(row)
  else if (cmd === 'suspend') openSuspend(row)
  else if (cmd === 'resume') doResume(row)
  else if (cmd === 'assign') openAssign(row)
}

function openSuspend(row: Task) {
  suspendTargetTask.value = row
  suspendReason.value = ''
  suspendDialogVisible.value = true
}

async function confirmSuspend() {
  const row = suspendTargetTask.value
  if (!row) return
  const reason = suspendReason.value.trim()
  if (!reason) {
    ElMessage.warning('请填写挂起原因')
    return
  }
  const userStoreInner = useUserStore()
  const operatorId = String(userStoreInner.userInfo?.id || '')
  try {
    await request.post(`/v1/prod/tasks/${row.id}/suspend`, {
      operatorId,
      reason,
      suspendType: 1
    })
    ElMessage.success('任务已挂起')
    suspendDialogVisible.value = false
    fetchTasks()
  } catch (err: any) {
    const message = err?.message || err?.response?.data?.message || err?.data?.message
    if (message) ElMessage.error(message)
  }
}

async function doResume(row: Task) {
  try {
    await ElMessageBox.confirm('确认恢复该任务到挂起前状态？', '恢复任务', { type: 'warning' })
  } catch {
    return
  }
  const operatorId = String(userStore.userInfo?.id || '')
  try {
    await request.post(`/v1/prod/tasks/${row.id}/resume`, { operatorId })
    ElMessage.success('任务已恢复')
    fetchTasks()
  } catch (err: any) {
    const message = err?.message || err?.response?.data?.message || err?.data?.message
    if (message) ElMessage.error(message)
  }
}

function openAssign(row: Task) {
  assignTargetTask.value = row
  assignUserId.value = null
  assignDialogVisible.value = true
}

async function confirmAssign() {
  const row = assignTargetTask.value
  if (!row || assignUserId.value == null) {
    ElMessage.warning('请选择新操作人')
    return
  }
  const picked = userOptions.value.find(u => u.id === assignUserId.value)
  try {
    await request.post(`/v1/prod/tasks/${row.id}/assign-operator`, {
      operatorId: String(assignUserId.value),
      operatorName: picked?.name
    })
    ElMessage.success('操作人已更新')
    assignDialogVisible.value = false
    fetchTasks()
  } catch (err: any) {
    const message = err?.message || err?.response?.data?.message || err?.data?.message
    if (message) ElMessage.error(message)
  }
}

async function startSoak(row: Task) {
  if (!hasTaskManage.value) {
    ElMessage.warning('无任务管理权限，无法在 Web 端开始泡药')
    return
  }
  const operatorId = String(userStore.userInfo?.id || '')
  try {
    await request.post(`/v1/prod/tasks/${row.id}/soak/start`, { operatorId })
    ElMessage.success('任务已开始泡药')
    try {
      await request.post('/v1/inv/consume/record', {
        taskId: row.id,
        operatorId,
        items: []
      })
    } catch {
      ElMessage.warning('消耗记录未写入（不影响任务推进）')
    }
    fetchTasks()
  } catch (err: any) {
    const message = err?.message || err?.response?.data?.message || err?.data?.message
    if (message) {
      ElMessage.error(message)
    }
  }
}

async function openDecoctDialog(row: Task) {
  if (!hasTaskManage.value) {
    ElMessage.warning('无任务管理权限，无法在 Web 端开始煎药')
    return
  }
  currentDecoctTask.value = row
  decoctDeviceCode.value = ''
  await fetchDecoctDevicesIdle()
  if (!decoctDevices.value.length) {
    ElMessage.warning(
      '未查到空闲煎药机（需 device_type=1 且状态 IDLE）。请核对设备是否占用、类型是否正确；有任务管理权限时应能看到全局空闲设备。'
    )
  }
  decoctDialogVisible.value = true
}

async function handleStartDecoct() {
  if (!hasTaskManage.value) {
    ElMessage.warning('无任务管理权限，无法在 Web 端开始煎药')
    return
  }
  if (!currentDecoctTask.value) return
  const deviceCode = decoctDeviceCode.value.trim()
  if (!deviceCode) {
    ElMessage.warning('请选择煎药设备')
    return
  }
  const operatorId = String(userStore.userInfo?.id || '')
  try {
    await startDecoct(currentDecoctTask.value.id, {
      deviceCode,
      operatorId: operatorId || undefined
    })
    ElMessage.success('任务已开始煎药')
    decoctDialogVisible.value = false
    fetchTasks()
  } catch (err: any) {
    const message = err?.message || err?.response?.data?.message || err?.data?.message
    if (message) {
      ElMessage.error(message)
    }
  }
}

function viewDetail(row: Task) {
  selectedTask.value = row
  detailVisible.value = true
}

onMounted(async () => {
  await fetchUserOptions()
  await fetchTasks()
})
</script>

<style scoped>
.decoct-group-tags { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }
.decoct-group-tag { margin: 0; }

.text-muted { color: var(--el-text-color-secondary); font-size: 13px; }
.col-hint { cursor: help; border-bottom: 1px dashed var(--el-text-color-secondary); }
.query-form { margin-bottom: 16px; }
.kanban-board { display: flex; gap: 12px; overflow-x: auto; padding-bottom: 8px; }
.kanban-column {
  flex: 0 0 220px;
  min-width: 220px;
  max-height: calc(100vh - 300px);
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color-page);
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter);
}
.kanban-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-weight: 500;
  font-size: 14px;
  background: var(--el-bg-color);
  border-radius: 8px 8px 0 0;
}
.kanban-body { flex: 1; overflow-y: auto; padding: 8px; }
.kanban-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 8px;
  cursor: pointer;
}
.kanban-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.kanban-card-id { font-weight: 600; font-size: 13px; color: var(--el-color-primary); }
.kanban-card-info { font-size: 12px; color: var(--el-text-color-regular); margin-bottom: 2px; }
.kanban-card-time { font-size: 11px; color: var(--el-text-color-secondary); margin-top: 4px; }
.kanban-card-actions { margin-top: 6px; display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }
</style>
