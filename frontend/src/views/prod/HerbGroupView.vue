<template>
  <div class="page-container">
    <el-page-header title="药材分组投料" content="按煎煮要求分组展示药材，扫码确认投料" />

    <el-card class="info-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>处方编号: {{ prescriptionId }}</span>
          <el-tag type="warning">待投料</el-tag>
        </div>
      </template>
      <el-descriptions :column="3">
        <el-descriptions-item label="患者">张三</el-descriptions-item>
        <el-descriptions-item label="剂数">7剂</el-descriptions-item>
        <el-descriptions-item label="煎法">常压煎煮</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <div class="group-grid">
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
            <span class="herb-dose">{{ herb.dose }}g</span>
          </div>
        </div>

        <div class="group-action">
          <el-button
            v-if="!group.confirmed"
            type="primary"
            :icon="FullScreen"
            @click="handleConfirm(group)"
          >
            扫码确认投料
          </el-button>
          <el-button v-else type="success" :icon="Check" disabled>
            已投料
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, FullScreen } from '@element-plus/icons-vue'
import { getHerbGroups, confirmHerbGroup } from '@/api/newModules'

interface Herb {
  id: number
  name: string
  dose: number
}

interface HerbGroup {
  id: number
  type: string
  name: string
  herbs: Herb[]
  confirmed: boolean
}

const route = useRoute()
const prescriptionId = computed(() => route.query.prescriptionId as string || '')

const groupList = ref<HerbGroup[]>([
  {
    id: 1,
    type: 'pre',
    name: '先煎组',
    confirmed: false,
    herbs: [
      { id: 101, name: '牡蛎', dose: 30 },
      { id: 102, name: '龙骨', dose: 20 }
    ]
  },
  {
    id: 2,
    type: 'main',
    name: '群煎组',
    confirmed: false,
    herbs: [
      { id: 201, name: '黄芪', dose: 15 },
      { id: 202, name: '当归', dose: 10 },
      { id: 203, name: '白术', dose: 12 },
      { id: 204, name: '茯苓', dose: 15 }
    ]
  },
  {
    id: 3,
    type: 'post',
    name: '后下组',
    confirmed: false,
    herbs: [
      { id: 301, name: '薄荷', dose: 6 },
      { id: 302, name: '砂仁', dose: 5 }
    ]
  },
  {
    id: 4,
    type: 'wrap',
    name: '包煎组',
    confirmed: false,
    herbs: [
      { id: 401, name: '车前子', dose: 10 },
      { id: 402, name: '旋覆花', dose: 8 }
    ]
  },
  {
    id: 5,
    type: 'melt',
    name: '烊化组',
    confirmed: false,
    herbs: [
      { id: 501, name: '阿胶', dose: 10 },
      { id: 502, name: '鹿角胶', dose: 6 }
    ]
  },
  {
    id: 6,
    type: 'infuse',
    name: '冲服组',
    confirmed: false,
    herbs: [
      { id: 601, name: '三七粉', dose: 3 },
      { id: 602, name: '川贝粉', dose: 2 }
    ]
  }
])

async function handleConfirm(group: HerbGroup) {
  try {
    await confirmHerbGroup(group.id)
    group.confirmed = true
    ElMessage.success(`${group.name} 投料确认成功`)
  } catch (e) {
    // handled by interceptor
  }
}

async function loadGroups() {
  if (!prescriptionId.value) return
  try {
    const res: any = await getHerbGroups(prescriptionId.value)
    const data = res.data || []
    if (data.length) {
      groupList.value = data
    }
  } catch (e) {
    ElMessage.error('加载药材分组失败')
  }
}

onMounted(() => {
  loadGroups()
})
</script>

<style scoped lang="scss">
.page-container {
  padding: var(--ygt-space-4);
}

.info-card {
  margin-top: 16px;
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 500;
}

.group-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.group-card {
  border-top: 4px solid transparent;
}

.group-pre { border-top-color: #f56c6c; }
.group-main { border-top-color: #409eff; }
.group-post { border-top-color: #67c23a; }
.group-wrap { border-top-color: #e6a23c; }
.group-melt { border-top-color: #9254de; }
.group-infuse { border-top-color: #909399; }

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.herb-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.herb-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: var(--ygt-bg-page);
  border-radius: var(--ygt-radius-sm);
}

.herb-name {
  font-weight: 500;
}

.herb-dose {
  color: var(--ygt-text-secondary);
  font-size: 14px;
}

.group-action {
  display: flex;
  justify-content: flex-end;
}
</style>
