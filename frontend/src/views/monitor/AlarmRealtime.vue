<template>
  <div class="alarm-realtime">
    <el-badge :value="unreadCount" :hidden="!unreadCount" class="alarm-badge">
      <el-button circle @click="showPanel = !showPanel">
        <el-icon><Bell /></el-icon>
      </el-button>
    </el-badge>

    <el-drawer v-model="showPanel" title="实时告警" size="400px">
      <div class="alarm-actions">
        <el-button link type="primary" @click="markAllRead">全部已读</el-button>
        <el-button link type="danger" @click="clearAll">清空</el-button>
      </div>
      <el-empty v-if="!alarmList.length" description="暂无告警" />
      <div v-else class="alarm-list">
        <div
          v-for="alarm in alarmList"
          :key="alarm.id"
          class="alarm-item"
          :class="{ unread: !alarm.read }"
          @click="handleAlarmClick(alarm)"
        >
          <div class="alarm-header">
            <el-tag size="small" :type="alarmLevelType(alarm.level)">{{ alarm.level }}</el-tag>
            <span class="alarm-time">{{ alarm.createTime }}</span>
          </div>
          <div class="alarm-title">{{ alarm.title }}</div>
          <div class="alarm-content">{{ alarm.content }}</div>
          <div class="alarm-device" v-if="alarm.deviceCode">
            <el-icon><Cpu /></el-icon> {{ alarm.deviceCode }}
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, Cpu } from '@element-plus/icons-vue'

const showPanel = ref(false)
const alarmList = ref<any[]>([])
let timer: ReturnType<typeof setInterval> | null = null

const unreadCount = computed(() => alarmList.value.filter(a => !a.read).length)

function alarmLevelType(level: string) {
  switch (level) {
    case 'CRITICAL': return 'danger'
    case 'WARNING': return 'warning'
    default: return 'info'
  }
}

function markAllRead() {
  alarmList.value.forEach(a => a.read = true)
}

function clearAll() {
  alarmList.value = []
}

function handleAlarmClick(alarm: any) {
  alarm.read = true
  if (alarm.deviceCode) {
    // 可导航到设备详情
  }
}

function addAlarm(alarm: any) {
  alarmList.value.unshift({ ...alarm, read: false })
  if (alarmList.value.length > 50) {
    alarmList.value = alarmList.value.slice(0, 50)
  }
}

function mockPoll() {
  // 模拟拉取告警，实际应连接 WebSocket
  // 生产环境替换为 WebSocket 监听
}

onMounted(() => {
  // 每30秒轮询一次（生产环境应使用WebSocket）
  timer = setInterval(mockPoll, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

// 暴露方法供外部调用
defineExpose({ addAlarm })
</script>

<style scoped lang="scss">
.alarm-realtime {
  .alarm-badge {
    margin-right: 12px;
  }

  .alarm-actions {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
  }

  .alarm-list {
    .alarm-item {
      padding: 12px;
      border-bottom: 1px solid #ebeef5;
      cursor: pointer;
      transition: background 0.2s;

      &:hover { background: #f5f7fa; }

      &.unread {
        background: #fff2f0;
        border-left: 3px solid #f56c6c;
      }

      .alarm-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 6px;

        .alarm-time { font-size: 12px; color: #999; }
      }

      .alarm-title { font-weight: bold; margin-bottom: 4px; }
      .alarm-content { font-size: 13px; color: #666; margin-bottom: 4px; }
      .alarm-device { font-size: 12px; color: #999; }
    }
  }
}
</style>
