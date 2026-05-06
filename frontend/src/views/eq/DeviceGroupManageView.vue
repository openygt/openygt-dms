<template>
  <div class="page-container">
    <div class="page-header-title">设备分组</div>
    <el-tabs v-model="activeTab" class="group-tabs">
      <el-tab-pane label="分组管理" name="group">
        <el-empty description="分组管理功能开发中" />
      </el-tab-pane>
      <el-tab-pane label="配对规则" name="pairing">
        <el-empty description="配对规则功能开发中" />
      </el-tab-pane>
      <el-tab-pane label="联动规则" name="rule">
        <div class="rule-toolbar">
          <el-button type="primary" @click="openDialog()">新增规则</el-button>
          <el-select v-model="filterGroupId" placeholder="按分组筛选" clearable style="width: 200px; margin-left: 12px;">
            <el-option v-for="g in groupList" :key="g.id" :label="g.groupName" :value="g.id" />
          </el-select>
          <el-button style="margin-left: 8px;" @click="loadRules">查询</el-button>
        </div>

        <el-table :data="ruleList" style="margin-top: 12px;" v-loading="loading" border>
          <el-table-column prop="ruleName" label="规则名称" min-width="140" />
          <el-table-column label="所属分组" min-width="120">
            <template #default="{ row }">
              {{ resolveGroupName(row.groupId) }}
            </template>
          </el-table-column>
          <el-table-column prop="actionType" label="动作类型" min-width="120">
            <template #default="{ row }">
              <el-tag>{{ formatActionType(row.actionType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="triggerCondition" label="触发条件" min-width="180" show-overflow-tooltip />
          <el-table-column prop="targetDevices" label="目标设备" min-width="180" show-overflow-tooltip />
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
          <el-table-column prop="createdAt" label="创建时间" width="160" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="deleteRule(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          style="margin-top: 12px;"
          @current-change="loadRules"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑规则' : '新增规则'" width="560px">
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="所属分组" prop="groupId">
          <el-select v-model="form.groupId" placeholder="选择设备分组" style="width: 100%">
            <el-option v-for="g in groupList" :key="g.id" :label="g.groupName" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="动作类型" prop="actionType">
          <el-select v-model="form.actionType" placeholder="选择动作类型" style="width: 100%">
            <el-option label="启动" value="START" />
            <el-option label="停止" value="STOP" />
            <el-option label="告警" value="ALARM" />
            <el-option label="通知" value="NOTIFY" />
            <el-option label="急停" value="EMERGENCY_STOP" />
          </el-select>
        </el-form-item>
        <el-form-item label="触发条件" prop="triggerCondition">
          <el-input
            v-model="form.triggerCondition"
            type="textarea"
            :rows="3"
            placeholder='JSON格式，例如: {"deviceStatus":"FAULT","durationSec":30}'
          />
        </el-form-item>
        <el-form-item label="目标设备" prop="targetDevices">
          <el-input
            v-model="form.targetDevices"
            type="textarea"
            :rows="3"
            placeholder='JSON格式设备编码列表，例如: ["JY-001","JY-002"]'
          />
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const activeTab = ref('rule')
const loading = ref(false)
const ruleList = ref<any[]>([])
const groupList = ref<any[]>([])
const filterGroupId = ref<number | undefined>(undefined)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<any>(null)
const form = reactive({
  id: undefined as number | undefined,
  ruleName: '',
  groupId: undefined as number | undefined,
  actionType: '',
  triggerCondition: '',
  targetDevices: '',
  enabled: 1
})

const rules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  groupId: [{ required: true, message: '请选择所属分组', trigger: 'change' }],
  actionType: [{ required: true, message: '请选择动作类型', trigger: 'change' }],
  triggerCondition: [{ required: true, message: '请输入触发条件', trigger: 'blur' }],
  targetDevices: [{ required: true, message: '请输入目标设备', trigger: 'blur' }]
}

onMounted(() => {
  loadGroups()
  loadRules()
})

async function loadGroups() {
  try {
    const res: any = await request.get('/v1/eq/groups/all')
    groupList.value = res.data || []
  } catch (e) {
    groupList.value = []
  }
}

async function loadRules() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (filterGroupId.value) {
      params.groupId = filterGroupId.value
    }
    const res: any = await request.get('/v1/eq/group-rules', { params })
    const data = res.data || {}
    ruleList.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    ruleList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function resolveGroupName(groupId: number) {
  const g = groupList.value.find((x: any) => x.id === groupId)
  return g ? g.groupName : groupId
}

function formatActionType(type: string) {
  const map: Record<string, string> = {
    START: '启动',
    STOP: '停止',
    ALARM: '告警',
    NOTIFY: '通知',
    EMERGENCY_STOP: '急停'
  }
  return map[type] || type
}

function openDialog(row?: any) {
  if (row) {
    isEdit.value = true
    form.id = row.id
    form.ruleName = row.ruleName
    form.groupId = row.groupId
    form.actionType = row.actionType
    form.triggerCondition = row.triggerCondition
    form.targetDevices = row.targetDevices
    form.enabled = row.enabled
  } else {
    isEdit.value = false
    form.id = undefined
    form.ruleName = ''
    form.groupId = undefined
    form.actionType = ''
    form.triggerCondition = ''
    form.targetDevices = ''
    form.enabled = 1
  }
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    if (isEdit.value && form.id) {
      await request.put(`/v1/eq/group-rules/${form.id}`, { ...form })
      ElMessage.success('规则已更新')
    } else {
      await request.post('/v1/eq/group-rules', { ...form })
      ElMessage.success('规则已创建')
    }
    dialogVisible.value = false
    loadRules()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function toggleRule(row: any) {
  try {
    await request.post(`/v1/eq/group-rules/${row.id}/toggle`)
    ElMessage.success('状态已切换')
    loadRules()
  } catch (e: any) {
    ElMessage.error(e.message || '切换失败')
    loadRules()
  }
}

async function deleteRule(row: any) {
  try {
    await ElMessageBox.confirm('确定删除该联动规则吗？删除后不可恢复。', '删除确认', { type: 'warning' })
    await request.delete(`/v1/eq/group-rules/${row.id}`)
    ElMessage.success('规则已删除')
    loadRules()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}
</script>

<style scoped lang="scss">
.page-container {
  padding: var(--ygt-space-4);
}
.group-tabs {
  margin-top: 16px;
}
.rule-toolbar {
  display: flex;
  align-items: center;
}
</style>
