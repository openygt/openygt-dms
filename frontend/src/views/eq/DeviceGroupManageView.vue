<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" class="group-tabs">
      <el-tab-pane label="分组管理" name="group">
        <div class="rule-toolbar">
          <el-button type="success" @click="openGroupDialog()">+ 新增分组</el-button>
          <el-input v-model="groupKeyword" placeholder="分组编码/名称" clearable style="width:200px;margin-left:12px;" />
          <el-button style="margin-left:8px;" @click="loadGroups">查询</el-button>
          <el-button @click="groupKeyword='';loadGroups()">重置</el-button>
        </div>
        <el-table :data="filteredGroups" style="margin-top:12px;" v-loading="groupLoading" border>
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="groupCode" label="分组编码" min-width="120" />
          <el-table-column prop="groupName" label="分组名称" min-width="140" />
          <el-table-column prop="groupType" label="分组类型" width="110">
            <template #default="{row}">
              <el-tag :type="groupTypeTag(row.groupType)">{{ groupTypeText(row.groupType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="包含设备数" width="100">
            <template #default="{row}">{{ groupDeviceCount(row.id) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{row}">
              <el-tag :type="row.status==='ACTIVE'?'success':'info'">{{ row.status==='ACTIVE'?'启用':'停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{row}">
              <el-button link type="primary" size="small" @click="openGroupDialog(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="deleteGroup(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!groupLoading && filteredGroups.length===0" description="暂无分组" />
      </el-tab-pane>

      <el-tab-pane label="配对规则" name="pairing">
        <div class="rule-toolbar">
          <el-button type="success" @click="openPairingDialog()">+ 新增配对</el-button>
          <el-button style="margin-left:8px;" @click="loadPairings">刷新</el-button>
        </div>
        <el-table :data="pairingList" style="margin-top:12px;" v-loading="pairingLoading" border>
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="pairingName" label="配对名称" min-width="140" />
          <el-table-column label="煎药机" min-width="180">
            <template #default="{row}">
              <div v-for="d in row.decocters" :key="d.id">{{ d.name }} ({{ d.deviceCode }})</div>
            </template>
          </el-table-column>
          <el-table-column label="包装机" min-width="160">
            <template #default="{row}">{{ row.packer?.name }} ({{ row.packer?.deviceCode }})</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{row}">
              <el-tag :type="row.status==='ACTIVE'?'success':'info'">{{ row.status==='ACTIVE'?'启用':'停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{row}">
              <el-button link type="primary" size="small" @click="openPairingDialog(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="deletePairing(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!pairingLoading && pairingList.length===0" description="暂无配对规则" />
      </el-tab-pane>

      <el-tab-pane label="联动规则" name="rule">
        <div class="rule-toolbar">
          <el-button type="primary" @click="openRuleDialog()">新增规则</el-button>
          <el-select v-model="filterGroupId" placeholder="按分组筛选" clearable style="width:200px;margin-left:12px;">
            <el-option v-for="g in allGroups" :key="g.id" :label="g.groupName" :value="g.id" />
          </el-select>
          <el-button style="margin-left:8px;" @click="loadRules">查询</el-button>
        </div>
        <el-table :data="ruleList" style="margin-top:12px;" v-loading="ruleLoading" border>
          <el-table-column prop="ruleName" label="规则名称" min-width="140" />
          <el-table-column label="所属分组" min-width="120">
            <template #default="{row}">{{ resolveGroupName(row.groupId) }}</template>
          </el-table-column>
          <el-table-column prop="actionType" label="动作类型" min-width="120">
            <template #default="{row}"><el-tag>{{ formatActionType(row.actionType) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="triggerCondition" label="触发条件" min-width="180" show-overflow-tooltip />
          <el-table-column prop="targetDevices" label="目标设备" min-width="180" show-overflow-tooltip />
          <el-table-column prop="enabled" label="状态" width="90">
            <template #default="{row}">
              <el-switch :model-value="row.enabled===1" active-text="启用" inactive-text="停用" inline-prompt @change="toggleRule(row)" />
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="160" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{row}">
              <el-button link type="primary" size="small" @click="openRuleDialog(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="deleteRule(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-model:current-page="rulePage" v-model:page-size="ruleSize" :total="ruleTotal"
          layout="total, prev, pager, next" style="margin-top:12px;" @current-change="loadRules" />
      </el-tab-pane>
    </el-tabs>

    <!-- 分组对话框 -->
    <el-dialog v-model="groupDialogVisible" :title="groupEdit?'编辑分组':'新增分组'" width="520px">
      <el-form :model="groupForm" label-width="100px" ref="groupFormRef">
        <el-form-item label="分组编码" required><el-input v-model="groupForm.groupCode" placeholder="大写英文+下划线" /></el-form-item>
        <el-form-item label="分组名称" required><el-input v-model="groupForm.groupName" placeholder="中文名称" /></el-form-item>
        <el-form-item label="分组类型">
          <el-select v-model="groupForm.groupType" placeholder="选择类型" style="width:100%">
            <el-option label="生产线" value="PRODUCTION_LINE" />
            <el-option label="车间" value="WORKSHOP" />
            <el-option label="区域" value="AREA" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="groupForm.remark" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="groupForm.status">
            <el-radio label="ACTIVE">启用</el-radio>
            <el-radio label="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialogVisible=false">取消</el-button>
        <el-button type="primary" @click="submitGroup">确定</el-button>
      </template>
    </el-dialog>

    <!-- 配对对话框 -->
    <el-dialog v-model="pairingDialogVisible" :title="pairingEdit?'编辑配对':'新增配对'" width="560px">
      <el-form :model="pairingForm" label-width="100px" ref="pairingFormRef">
        <el-form-item label="配对名称" required><el-input v-model="pairingForm.pairingName" /></el-form-item>
        <el-form-item label="煎药机" required>
          <el-select v-model="pairingForm.decocterIds" multiple placeholder="选择煎药机" style="width:100%">
            <el-option v-for="d in decoctDevices" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="包装机" required>
          <el-select v-model="pairingForm.packerId" placeholder="选择包装机" style="width:100%">
            <el-option v-for="d in packDevices" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="pairingForm.status">
            <el-radio label="ACTIVE">启用</el-radio>
            <el-radio label="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pairingDialogVisible=false">取消</el-button>
        <el-button type="primary" @click="submitPairing">确定</el-button>
      </template>
    </el-dialog>

    <!-- 联动规则对话框 -->
    <el-dialog v-model="ruleDialogVisible" :title="ruleEdit?'编辑规则':'新增规则'" width="560px">
      <el-form :model="ruleForm" label-width="100px" :rules="ruleRules" ref="ruleFormRef">
        <el-form-item label="规则名称" prop="ruleName"><el-input v-model="ruleForm.ruleName" /></el-form-item>
        <el-form-item label="所属分组" prop="groupId">
          <el-select v-model="ruleForm.groupId" placeholder="选择分组" style="width:100%"><el-option v-for="g in allGroups" :key="g.id" :label="g.groupName" :value="g.id" /></el-select>
        </el-form-item>
        <el-form-item label="动作类型" prop="actionType">
          <el-select v-model="ruleForm.actionType" placeholder="选择动作" style="width:100%"><el-option label="启动" value="START" /><el-option label="停止" value="STOP" /><el-option label="告警" value="ALARM" /><el-option label="通知" value="NOTIFY" /><el-option label="急停" value="EMERGENCY_STOP" /></el-select>
        </el-form-item>
        <el-form-item label="触发条件" prop="triggerCondition"><el-input v-model="ruleForm.triggerCondition" type="textarea" :rows="3" placeholder='JSON格式' /></el-form-item>
        <el-form-item label="目标设备" prop="targetDevices"><el-input v-model="ruleForm.targetDevices" type="textarea" :rows="3" placeholder='JSON格式设备编码列表' /></el-form-item>
        <el-form-item label="是否启用"><el-switch v-model="ruleForm.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="ruleDialogVisible=false">取消</el-button><el-button type="primary" @click="submitRule">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const activeTab = ref('group')

/* ========== 分组管理 ========== */
const groupLoading = ref(false)
const groupList = ref<any[]>([])
const groupKeyword = ref('')
const groupDialogVisible = ref(false)
const groupEdit = ref(false)
const groupFormRef = ref<any>(null)
const groupForm = reactive({ id: undefined as number|undefined, groupCode:'', groupName:'', groupType:'PRODUCTION_LINE', remark:'', status:'ACTIVE' })

const filteredGroups = computed(() => {
  const kw = groupKeyword.value.trim().toLowerCase()
  if (!kw) return groupList.value
  return groupList.value.filter(g => (g.groupCode||'').toLowerCase().includes(kw) || (g.groupName||'').toLowerCase().includes(kw))
})

function groupTypeText(type?: string) {
  const map: Record<string,string> = { PRODUCTION_LINE:'生产线', WORKSHOP:'车间', AREA:'区域', OTHER:'其他' }
  return map[type||''] || type || '-'
}
function groupTypeTag(type?: string) {
  const map: Record<string,string> = { PRODUCTION_LINE:'success', WORKSHOP:'warning', AREA:'primary', OTHER:'info' }
  return map[type||''] || 'info'
}
const devices = ref<any[]>([])
function groupDeviceCount(groupId: number) {
  return devices.value.filter(d => d.groupId === groupId).length
}

async function loadDevices() {
  try {
    const res: any = await request.get('/v1/eq/devices', { params: { page:1, size:200 } })
    devices.value = res.data?.records || []
  } catch { devices.value = [] }
}

async function loadGroups() {
  groupLoading.value = true
  try {
    const res: any = await request.get('/v1/eq/groups', { params: { page:1, size:50, keyword: groupKeyword.value } })
    groupList.value = res.data?.records || []
  } catch { groupList.value = [] } finally { groupLoading.value = false }
}

function openGroupDialog(row?: any) {
  if (row) {
    groupEdit.value = true
    Object.assign(groupForm, { ...row })
  } else {
    groupEdit.value = false
    Object.assign(groupForm, { id: undefined, groupCode:'', groupName:'', groupType:'PRODUCTION_LINE', remark:'', status:'ACTIVE' })
  }
  groupDialogVisible.value = true
}

async function submitGroup() {
  if (!groupForm.groupCode || !groupForm.groupName) { ElMessage.warning('请填写编码和名称'); return }
  try {
    if (groupEdit.value && groupForm.id) {
      await request.put(`/v1/eq/groups/${groupForm.id}`, { ...groupForm })
      ElMessage.success('分组已更新')
    } else {
      await request.post('/v1/eq/groups', { ...groupForm })
      ElMessage.success('分组已创建')
    }
    groupDialogVisible.value = false
    loadGroups()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

async function deleteGroup(row: any) {
  try {
    await ElMessageBox.confirm('删除分组后，组内设备将变为"未分组"', '删除确认', { type: 'warning' })
    await request.delete(`/v1/eq/groups/${row.id}`)
    ElMessage.success('分组已删除')
    loadGroups(); loadDevices()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

/* ========== 配对规则 ========== */
const pairingLoading = ref(false)
const pairingList = ref<any[]>([])
const pairingDialogVisible = ref(false)
const pairingEdit = ref(false)
const pairingFormRef = ref<any>(null)
const pairingForm = reactive({ id: undefined as number|undefined, pairingName:'', decocterIds: [] as number[], packerId: undefined as number|undefined, status:'ACTIVE' })
const decoctDevices = computed(() => devices.value.filter(d => d.deviceType === 1))
const packDevices = computed(() => devices.value.filter(d => d.deviceType === 2))

async function loadPairings() {
  pairingLoading.value = true
  try {
    const res: any = await request.get('/v1/eq/pairings')
    pairingList.value = res.data || []
  } catch { pairingList.value = [] } finally { pairingLoading.value = false }
}

function openPairingDialog(row?: any) {
  if (row) {
    pairingEdit.value = true
    Object.assign(pairingForm, { id: row.id, pairingName: row.pairingName, decocterIds: row.decocterIds || [], packerId: row.packer?.id, status: row.status })
  } else {
    pairingEdit.value = false
    Object.assign(pairingForm, { id: undefined, pairingName:'', decocterIds:[], packerId: undefined, status:'ACTIVE' })
  }
  pairingDialogVisible.value = true
}

async function submitPairing() {
  if (!pairingForm.pairingName || !pairingForm.packerId) { ElMessage.warning('请填写名称并选择包装机'); return }
  try {
    const payload = { ...pairingForm, decocterIds: pairingForm.decocterIds }
    if (pairingEdit.value && pairingForm.id) {
      await request.put(`/v1/eq/pairings/${pairingForm.id}`, payload)
      ElMessage.success('配对已更新')
    } else {
      await request.post('/v1/eq/pairings', payload)
      ElMessage.success('配对已创建')
    }
    pairingDialogVisible.value = false
    loadPairings()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

async function deletePairing(row: any) {
  try {
    await ElMessageBox.confirm('确定删除该配对规则吗？', '删除确认', { type: 'warning' })
    await request.delete(`/v1/eq/pairings/${row.id}`)
    ElMessage.success('配对已删除')
    loadPairings()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

/* ========== 联动规则（保留） ========== */
const ruleLoading = ref(false)
const ruleList = ref<any[]>([])
const allGroups = ref<any[]>([])
const filterGroupId = ref<number|undefined>(undefined)
const rulePage = ref(1)
const ruleSize = ref(10)
const ruleTotal = ref(0)
const ruleDialogVisible = ref(false)
const ruleEdit = ref(false)
const ruleFormRef = ref<any>(null)
const ruleForm = reactive({ id: undefined as number|undefined, ruleName:'', groupId: undefined as number|undefined, actionType:'', triggerCondition:'', targetDevices:'', enabled: 1 })
const ruleRules = {
  ruleName: [{ required:true, message:'请输入规则名称', trigger:'blur' }],
  groupId: [{ required:true, message:'请选择分组', trigger:'change' }],
  actionType: [{ required:true, message:'请选择动作类型', trigger:'change' }],
  triggerCondition: [{ required:true, message:'请输入触发条件', trigger:'blur' }],
  targetDevices: [{ required:true, message:'请输入目标设备', trigger:'blur' }]
}

async function loadAllGroups() {
  try { const res: any = await request.get('/v1/eq/groups/all'); allGroups.value = res.data || [] } catch { allGroups.value = [] }
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
  const g = allGroups.value.find((x:any) => x.id === groupId)
  return g ? g.groupName : groupId
}
function formatActionType(type: string) {
  const map: Record<string,string> = { START:'启动', STOP:'停止', ALARM:'告警', NOTIFY:'通知', EMERGENCY_STOP:'急停' }
  return map[type] || type
}
function openRuleDialog(row?: any) {
  if (row) {
    ruleEdit.value = true
    Object.assign(ruleForm, { id:row.id, ruleName:row.ruleName, groupId:row.groupId, actionType:row.actionType, triggerCondition:row.triggerCondition, targetDevices:row.targetDevices, enabled:row.enabled })
  } else {
    ruleEdit.value = false
    Object.assign(ruleForm, { id:undefined, ruleName:'', groupId:undefined, actionType:'', triggerCondition:'', targetDevices:'', enabled:1 })
  }
  ruleDialogVisible.value = true
}
async function submitRule() {
  const valid = await ruleFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    if (ruleEdit.value && ruleForm.id) { await request.put(`/v1/eq/group-rules/${ruleForm.id}`, { ...ruleForm }); ElMessage.success('规则已更新') }
    else { await request.post('/v1/eq/group-rules', { ...ruleForm }); ElMessage.success('规则已创建') }
    ruleDialogVisible.value = false
    loadRules()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}
async function toggleRule(row: any) {
  try { await request.post(`/v1/eq/group-rules/${row.id}/toggle`); ElMessage.success('状态已切换'); loadRules() }
  catch { loadRules() }
}
async function deleteRule(row: any) {
  try {
    await ElMessageBox.confirm('确定删除该联动规则吗？', '删除确认', { type: 'warning' })
    await request.delete(`/v1/eq/group-rules/${row.id}`)
    ElMessage.success('规则已删除'); loadRules()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

onMounted(() => { loadDevices(); loadGroups(); loadPairings(); loadAllGroups(); loadRules() })
</script>

<style scoped lang="scss">
.page-container { padding: 0; }
.group-tabs { margin-top: 0; }
.rule-toolbar { display: flex; align-items: center; }
</style>
