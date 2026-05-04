<template>
  <div class="page-container">
    <div class="page-header-title">分组投料：<span class="page-header-sub">先煎/群煎/后下/冲服、扫码确认、倒计时提醒</span></div>

    <el-card class="info-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>处方编号: {{ prescriptionId || '-' }}</span>
          <el-button size="small" @click="loadGroups">刷新</el-button>
        </div>
      </template>
      <el-descriptions :column="3">
        <el-descriptions-item label="患者">{{ prescriptionInfo.patientName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="剂数">{{ prescriptionInfo.doseCount || '-' }}</el-descriptions-item>
        <el-descriptions-item label="煎法">{{ prescriptionInfo.schemeName || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <div v-if="!groupList.length" class="empty-wrap">
      <el-empty description="暂无药材分组" :image-size="72" />
    </div>

    <div v-else class="group-grid">
      <el-card
        v-for="group in groupList"
        :key="group.id"
        class="group-card"
        :class="`group-${group.type}`"
        shadow="hover"
      >
        <template #header>
          <div class="group-header">
            <div class="group-title">
              <el-icon :size="18"><FirstAidKit /></el-icon>
              <span>{{ group.name }}</span>
            </div>
            <el-tag v-if="group.confirmed" type="success">已确认</el-tag>
            <el-tag v-else type="info">待确认</el-tag>
          </div>
        </template>

        <div class="herb-list">
          <div v-for="herb in group.herbs" :key="herb.id" class="herb-item">
            <span class="herb-name">{{ herb.name }}</span>
            <span class="herb-dose">{{ herb.dose }}</span>
          </div>
        </div>

        <div class="group-action">
          <el-button v-if="!group.confirmed" type="primary" @click="handleConfirm(group)">扫码确认投料</el-button>
          <el-button v-else type="success" disabled>已投料</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { FirstAidKit } from '@element-plus/icons-vue'
import request from '@/api/request'
import { confirmHerbGroup, getHerbGroups } from '@/api/newModules'

interface HerbItem {
  id: number
  name: string
  dose: string
}

interface HerbGroupItem {
  id: number
  type: string
  name: string
  confirmed: boolean
  herbs: HerbItem[]
}

const route = useRoute()
const prescriptionId = computed(() => String(route.query.prescriptionId || route.params.prescriptionId || ''))

const groupList = ref<HerbGroupItem[]>([])
const prescriptionInfo = ref<{ patientName?: string; doseCount?: string; schemeName?: string }>({})

function mapGroupType(processType?: string, groupCode?: string) {
  const key = (processType || groupCode || '').toUpperCase()
  if (key.includes('PRE')) return 'pre'
  if (key.includes('POST')) return 'post'
  if (key.includes('WRAP')) return 'wrap'
  if (key.includes('MELT')) return 'melt'
  if (key.includes('DIRECT')) return 'infuse'
  return 'main'
}

function normalizeGroup(item: any): HerbGroupItem {
  const confirmed = Number(item.processStatus || 0) === 1
  return {
    id: item.id,
    type: mapGroupType(item.processType, item.groupCode),
    name: item.groupName || item.groupCode || `分组-${item.id}`,
    confirmed,
    herbs: (item.herbs || []).map((herb: any, index: number) => ({
      id: index + 1,
      name: herb.herbName || herb.name || '-',
      dose: `${herb.dosage || herb.dose || '-'}${herb.unit || ''}`
    }))
  }
}

async function loadPrescriptionInfo() {
  if (!prescriptionId.value) return
  try {
    const res: any = await request.get(`/v1/md/prescriptions/${prescriptionId.value}`)
    const data = res.data || {}
    prescriptionInfo.value = {
      patientName: data.patientName,
      doseCount: data.totalDose ? `${data.totalDose}剂` : data.doseCount,
      schemeName: data.schemeName || data.decoctionPlan || data.scheme?.schemeName
    }
  } catch {
    prescriptionInfo.value = {}
  }
}

async function loadGroups() {
  if (!prescriptionId.value) {
    groupList.value = []
    return
  }
  const res: any = await getHerbGroups(prescriptionId.value)
  groupList.value = (res.data || []).map(normalizeGroup)
}

async function handleConfirm(group: HerbGroupItem) {
  await confirmHerbGroup(group.id, {})
  group.confirmed = true
  ElMessage.success(`${group.name} 投料确认成功`)
}

onMounted(async () => {
  await Promise.all([loadPrescriptionInfo(), loadGroups()])
})
</script>

<style scoped lang="scss">
.page-container { padding: var(--ygt-space-4); }
.info-card { margin: 16px 0; }
.card-header,
.group-header,
.group-title,
.group-action { display: flex; align-items: center; }
.card-header,
.group-header { justify-content: space-between; }
.group-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
.group-card { border-top: 4px solid transparent; }
.group-pre { border-top-color: #f56c6c; }
.group-main { border-top-color: #409eff; }
.group-post { border-top-color: #67c23a; }
.group-wrap { border-top-color: #e6a23c; }
.group-melt { border-top-color: #9254de; }
.group-infuse { border-top-color: #909399; }
.group-title { gap: 8px; font-weight: 500; }
.herb-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; }
.herb-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: var(--ygt-radius-sm);
  background: var(--ygt-bg-page);
}
.herb-name { font-weight: 500; }
.herb-dose { color: var(--ygt-text-secondary); }
.group-action { justify-content: flex-end; }
.empty-wrap { padding: 24px 0; }
</style>
