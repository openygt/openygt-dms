<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="item in stats" :key="item.label">
        <el-card>
          <div style="font-size: 14px; color: #666">{{ item.label }}</div>
          <div style="font-size: 28px; font-weight: bold; margin-top: 8px">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card style="margin-top: 16px">
      <template #header>
        <span>欢迎</span>
      </template>
      <p>OpenYGT 智能煎药管理系统 V2.0</p>
      <p>当前角色：{{ userStore.userInfo?.roles?.join(', ') || '-' }}</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
const userStore = useUserStore()
const stats = ref([
  { label: '今日任务', value: 0 },
  { label: '待煎药', value: 0 },
  { label: '待质检', value: 0 },
  { label: '告警中', value: 0 }
])
</script>
