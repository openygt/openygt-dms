<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" class="group-tabs">
      <!-- ========== Tab 1: 配对规格 ========== -->
      <el-tab-pane label="配对规格" name="spec">
        <el-card shadow="never" class="spec-card">
          <template #header><span style="font-weight: 600">标准产线规格</span></template>
          <el-table :data="specList" border v-loading="specLoading" style="width: 100%">
            <el-table-column prop="specCode" label="规格编码" width="140" />
            <el-table-column prop="specName" label="规格名称" width="160" />
            <el-table-column label="煎药机数量" width="110">
              <template #default="{ row }">{{ row.decocterCount }} 台</template>
            </el-table-column>
            <el-table-column label="包装机数量" width="110">
              <template #default="{ row }">{{ row.packerCount }} 台</template>
            </el-table-column>
            <el-table-column label="标签打印机数量" width="130">
              <template #default="{ row }">{{ row.printerCount }} 台</template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="220" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
                  {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ========== Tab 2: 分组管理 ========== -->
      <el-tab-pane label="分组管理" name="group">
        <div class="toolbar">
          <el-button type="primary" @click="openGroupDialog()">+ 新增分组</el-button>
          <el-input v-model="groupKeyword" placeholder="分组编码/名称" clearable style="width:200px;margin-left:12px" />
          <el-button style="margin-left:8px" @click="loadGroups">查询</el-button>
          <el-button @click="groupKeyword='';loadGroups()">重置</el-button>
        </div>
        <el-table :data="filteredGroups" style="margin-top:12px" v-loading="groupLoading" border>
          <el-table-column prop="groupCode" label="分组编码" width="120" />
          <el-table-column prop="groupName" label="分组名称" width="100" />
          <el-table-column label="选用规格" width="160">
            <template #default>
              <el-tag type="success">标准产线规格 (4:1:1)</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="煎药机" min-width="220">
            <template #default="{ row }">
              <el-tag v-for="d in resolveDecocters(row.decocterIds)" :key="d.id" size="small" style="margin-right:4px;margin-bottom:2px">
                {{ d.name || d.deviceCode }}
              </el-tag>
              <span v-if="!resolveDecocters(row.decocterIds).length" style="color:#999">--</span>
            </template>
          </el-table-column>
          <el-table-column label="包装机" width="140">
            <template #default="{ row }">
              {{ resolveDeviceName(row.packageDeviceId) || '--' }}
            </template>
          </el-table-column>
          <el-table-column label="标签打印机" width="140">
            <template #default="{ row }">
              {{ resolveDeviceName(row.printerDeviceId) || '--' }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
                {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openGroupDialog(row)">编辑</el-button>
              <el-popconfirm title="删除分组后，组内设备将变为未分组" @confirm="deleteGroup(row)">
                <template #reference>
                  <el-button link type="danger" size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!groupLoading && filteredGroups.length === 0" description="暂无分组" />

        <!-- 分组对话框 -->
        <el-dialog v-model="groupDialogVisible" :title="groupEdit ? '编辑分组' : '新增分组'" width="560px">
          <el-form :model="groupForm" label-width="120px" ref="groupFormRef">
            <el-form-item label="分组编码" required>
              <el-input v-model="groupForm.groupCode" placeholder="大写英文+下划线" />
            </el-form-item>
            <el-form-item label="分组名称" required>
              <el-input v-model="groupForm.groupName" placeholder="如：A组" />
            </el-form-item>
            <el-form-item label="选用规格">
              <el-input :model-value="'标准产线规格 4:1:1 (4煎药机+1包装机+1打印机)'" disabled />
            </el-form-item>
            <el-form-item label="选择煎药机" required>
              <el-select
                v-model="groupForm.decocterIds"
                multiple
                :multiple-limit="4"
                placeholder="请选择4台煎药机"
                style="width:100%"
              >
                <el-option
                  v-for="d in decoctDevices"
                  :key="d.id"
                  :label="`${d.name || d.deviceCode} (${d.deviceCode})`"
                  :value="d.id"
                />
              </el-select>
              <div style="color:#909399;font-size:12px;margin-top:4px">
                已选 {{ (groupForm.decocterIds || []).length }} / 4 台
              </div>
            </el-form-item>
            <el-form-item label="包装机" required>
              <el-select v-model="groupForm.packageDeviceId" placeholder="请选择包装机" style="width:100%">
                <el-option
                  v-for="d in packDevices"
                  :key="d.id"
                  :label="`${d.name || d.deviceCode} (${d.deviceCode})`"
                  :value="d.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="标签打印机" required>
              <el-select v-model="groupForm.printerDeviceId" placeholder="请选择标签打印机" style="width:100%">
                <el-option
                  v-for="d in printerDevices"
                  :key="d.id"
                  :label="`${d.name || d.deviceCode} (${d.deviceCode})`"
                  :value="d.id"
                />
              </el-select>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="groupDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitGroup">确定</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- ========== Tab 3: 联动规则 ========== -->
      <el-tab-pane label="联动规则" name="rule">
        <div class="toolbar">
          <el-button type="primary" @click="openRuleDialog()">新增规则</el-button>
          <el-select v-model="filterGroupId" placeholder="按分组筛选" clearable style="width:200px;margin-left:12px">
            <el-option v-for="g in allGroups" :key="g.id" :label="g.groupName" :value="g.id" />
          </el-select>
          <el-button style="margin-left:8px" @click="loadRules">查询</el-button>
        </div>
        <el-table :data="ruleList" style="margin-top:12px" v-loading="ruleLoading" border>
          <el-table-column prop="ruleName" label="规则名称" min-width="140" />
          <el-table-column label="所属分组" min-width="120">
            <template #default="{ row }">{{ resolveGroupName(row.groupId) }}</template>
          </el-table-column>
          <el-table-column label="触发条件" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <el-tag>{{ formatTriggerCondition(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="动作类型" width="100">
            <template #default="{ row }">
              <el-tag :type="actionTag(row.actionType)">{{ formatActionType(row.actionType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="通知对象" min-width="150">
            <template #default="{ row }">{{ formatTargetDevices(row) }}</template>
          </el-table-column>
          <el-table-column prop="enabled" label="状态" width="90">
            <template #default="{ row }">
              <el-switch
                :model-value="row.enabled === 1"
                active-text="启用"
                inactive-text="停用"
                inline-prompt
                @change="toggleRule(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openRuleDialog(row)">编辑</el-button>
              <el-popconfirm title="确定删除该联动规则吗？" @confirm="deleteRule(row)">
                <template #reference>
                  <el-button link type="danger" size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="rulePage"
          v-model:page-size="ruleSize"
          :total="ruleTotal"
          layout="total, prev, pager, next"
          style="margin-top:12px"
          @current-change="loadRules"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const activeTab = ref('spec')

/* ========== 规格 ========== */
const specLoading = ref(false)
const specList = ref<any[]>([])

async function loadSpecs() {
  specLoading.value = true
  try {
    const res: any = await request.get('/v1/eq/groups/specs')
    specList.value = res.data || []
  } catch { specList.value = [] } finally { specLoading.value = false }
}

/* ========== 设备缓存 ========== */
const devices = ref<any[]>([])

const decoctDevices = computed(() => devices.value.filter((d: any) => d.deviceType === 1))
const packDevices = computed(() => devices.value.filter((d: any) => d.deviceType === 2))
const printerDevices = computed(() => devices.value.filter((d: any) => d.deviceType === 3))

async function loadDevices() {
  try {
    const res: any = await request.get('/v1/eq/devices', { params: { page: 1, size: 200 } })
    devices.value = res.data?.records || []
  } catch { devices.value = [] }
}

function resolveDeviceName(id: number | undefined | null) {
  if (!id) return ''
  const d = devices.value.find((x: any) => x.id === id)
  return d ? `${d.name || d.deviceCode}` : ''
}

function resolveDecocters(ids: string | undefined | null) {
  if (!ids) return []
  const idArr = ids.split(',').map(Number)
  return idArr.map((id: number) => devices.value.find((d: any) => d.id === id)).filter(Boolean)
}

/* ========== 分组管理 ========== */
const groupLoading = ref(false)
const groupList = ref<any[]>([])
const groupKeyword = ref('')
const groupDialogVisible = ref(false)
const groupEdit = ref(false)
const groupFormRef = ref<any>(null)
const groupForm = reactive<Record<string, any>>({
  id: undefined,
  groupCode: '',
  groupName: '',
  specId: 1,
  decocterIds: [] as number[],
  packageDeviceId: undefined as number | undefined,
  printerDeviceId: undefined as number | undefined,
})

const filteredGroups = computed(() => {
  const kw = groupKeyword.value.trim().toLowerCase()
  if (!kw) return groupList.value
  return groupList.value.filter(
    (g: any) =>
      (g.groupCode || '').toLowerCase().includes(kw) ||
      (g.groupName || '').toLowerCase().includes(kw)
  )
})

async function loadGroups() {
  groupLoading.value = true
  try {
    const res: any = await request.get('/v1/eq/groups', {
      params: { page: 1, size: 50, keyword: groupKeyword.value },
    })
    groupList.value = res.data?.records || []
  } catch { groupList.value = [] } finally { groupLoading.value = false }
}

function openGroupDialog(row?: any) {
  if (row) {
    groupEdit.value = true
    const decIds = row.decocterIds
      ? row.decocterIds.split(',').map(Number)
      : []
    groupForm.id = row.id
    groupForm.groupCode = row.groupCode
    groupForm.groupName = row.groupName
    groupForm.specId = row.specId || 1
    groupForm.decocterIds = decIds
    groupForm.packageDeviceId = row.packageDeviceId
    groupForm.printerDeviceId = row.printerDeviceId
  } else {
    groupEdit.value = false
    groupForm.id = undefined
    groupForm.groupCode = ''
    groupForm.groupName = ''
    groupForm.specId = 1
    groupForm.decocterIds = []
    groupForm.packageDeviceId = undefined
    groupForm.printerDeviceId = undefined
  }
  groupDialogVisible.value = true
}

async function submitGroup() {
  if (!groupForm.groupCode || !groupForm.groupName) {
    ElMessage.warning('请填写分组编码和名称')
    return
  }
  if (!groupForm.decocterIds || groupForm.decocterIds.length !== 4) {
    ElMessage.warning('请选择4台煎药机')
    return
  }
  if (!groupForm.packageDeviceId) {
    ElMessage.warning('请选择包装机')
    return
  }
  if (!groupForm.printerDeviceId) {
    ElMessage.warning('请选择标签打印机')
    return
  }
  try {
    const payload = {
      groupCode: groupForm.groupCode,
      groupName: groupForm.groupName,
      groupType: 'PRODUCTION_LINE',
      specId: 1,
      decocterIds: groupForm.decocterIds.join(','),
      packageDeviceId: groupForm.packageDeviceId,
      printerDeviceId: groupForm.printerDeviceId,
      status: 'ACTIVE',
    }
    if (groupEdit.value && groupForm.id) {
      await request.put(`/v1/eq/groups/${groupForm.id}`, payload)
      ElMessage.success('分组已更新')
    } else {
      await request.post('/v1/eq/groups', payload)
      ElMessage.success('分组已创建')
    }
    groupDialogVisible.value = false
    loadGroups()
    loadDevices()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

async function deleteGroup(row: any) {
  try {
    await request.delete(`/v1/eq/groups/${row.id}`)
    ElMessage.success('分组已删除')
    loadGroups()
    loadDevices()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

/* ========== 联动规则 ========== */
const ruleLoading = ref(false)
const ruleList = ref<any[]>([])
const allGroups = ref<any[]>([])
const filterGroupId = ref<number | undefined>(undefined)
const rulePage = ref(1)
const ruleSize = ref(10)
const ruleTotal = ref(0)
const ruleDialogVisible = ref(false)
const ruleEdit = ref(false)
const ruleFormRef = ref<any>(null)
const ruleForm = reactive({
  id: undefined as number | undefined,
  ruleName: '',
  groupId: undefined as number | undefined,
  actionType: '',
  triggerCondition: '',
  targetDevices: '',
  enabled: 1,
})
const ruleRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  groupId: [{ required: true, message: '请选择分组', trigger: 'change' }],
  actionType: [{ required: true, message: '请选择动作类型', trigger: 'change' }],
  triggerCondition: [{ required: true, message: '请输入触发条件', trigger: 'blur' }],
  targetDevices: [{ required: true, message: '请输入目标设备', trigger: 'blur' }],
}

async function loadAllGroups() {
  try {
    const res: any = await request.get('/v1/eq/groups/all')
    allGroups.value = res.data || []
  } catch { allGroups.value = [] }
}

async function loadRules() {
  ruleLoading.value = true
  try {
    const params: any = { page: rulePage.value, size: ruleSize.value }
    if (filterGroupId.value) params.groupId = filterGroupId.value
    const res: any = await request.get('/v1/eq/group-rules', { params })
    const data = res.data || {}
    ruleList.value = data.records || []
    ruleTotal.value = data.total || 0
  } catch { ruleList.value = []; ruleTotal.value = 0 } finally { ruleLoading.value = false }
}

function resolveGroupName(groupId: number) {
  const g = allGroups.value.find((x: any) => x.id === groupId)
  return g ? g.groupName : String(groupId)
}

function formatActionType(type: string) {
  const map: Record<string, string> = {
    START: '启动', STOP: '停止', ALARM: '告警', NOTIFY: '通知', EMERGENCY_STOP: '急停',
  }
  return map[type] || type
}

function actionTag(type: string) {
  const map: Record<string, string> = {
    ALARM: 'danger', NOTIFY: 'primary', START: 'success', STOP: 'warning', EMERGENCY_STOP: 'danger',
  }
  return map[type] || 'info'
}

function formatTriggerCondition(row: any) {
  try {
    const obj = typeof row.triggerCondition === 'string' ? JSON.parse(row.triggerCondition) : row.triggerCondition
    return obj?.event || obj?.condition || row.triggerCondition
  } catch {
    return row.triggerCondition
  }
}

function formatTargetDevices(row: any) {
  try {
    const arr = typeof row.targetDevices === 'string' ? JSON.parse(row.targetDevices) : row.targetDevices
    if (Array.isArray(arr)) return arr.join('、')
    return row.targetDevices || '-'
  } catch {
    return row.targetDevices || '-'
  }
}

async function openRuleDialog(row?: any) {
  if (row) {
    ruleEdit.value = true
    Object.assign(ruleForm, {
      id: row.id,
      ruleName: row.ruleName,
      groupId: row.groupId,
      actionType: row.actionType,
      triggerCondition: row.triggerCondition,
      targetDevices: row.targetDevices,
      enabled: row.enabled,
    })
  } else {
    ruleEdit.value = false
    Object.assign(ruleForm, {
      id: undefined, ruleName: '', groupId: undefined,
      actionType: '', triggerCondition: '', targetDevices: '', enabled: 1,
    })
  }
  ruleDialogVisible.value = true
}

async function submitRule() {
  const valid = await ruleFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    if (ruleEdit.value && ruleForm.id) {
      await request.put(`/v1/eq/group-rules/${ruleForm.id}`, { ...ruleForm })
      ElMessage.success('规则已更新')
    } else {
      await request.post('/v1/eq/group-rules', { ...ruleForm })
      ElMessage.success('规则已创建')
    }
    ruleDialogVisible.value = false
    loadRules()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

async function toggleRule(row: any) {
  try {
    await request.post(`/v1/eq/group-rules/${row.id}/toggle`)
    ElMessage.success('状态已切换')
    loadRules()
  } catch { loadRules() }
}

async function deleteRule(row: any) {
  try {
    await request.delete(`/v1/eq/group-rules/${row.id}`)
    ElMessage.success('规则已删除')
    loadRules()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '删除失败')
  }
}

onMounted(() => {
  loadSpecs()
  loadDevices()
  loadGroups()
  loadAllGroups()
  loadRules()
})
</script>

<style scoped lang="scss">
.page-container { padding: 0; }
.group-tabs { margin-top: 0; }
.toolbar { display: flex; align-items: center; }
.spec-card { margin-top: 0; }
</style>
