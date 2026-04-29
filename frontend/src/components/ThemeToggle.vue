<template>
  <el-dropdown @command="setTheme" trigger="click">
    <el-button text :icon="currentIcon" circle :title="`当前主题: ${themeLabel}`" />
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="light" :icon="Sunny">浅色模式</el-dropdown-item>
        <el-dropdown-item command="dark" :icon="Moon">深色模式</el-dropdown-item>
        <el-dropdown-item command="auto" :icon="Aim">跟随系统</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Sunny, Moon, Aim } from '@element-plus/icons-vue'
import { useTheme } from '@/composables/useTheme'

const { theme, setTheme } = useTheme()

const currentIcon = computed(() => {
  if (theme.value === 'dark') return Moon
  if (theme.value === 'light') return Sunny
  return Aim
})

const themeLabel = computed(() => {
  if (theme.value === 'dark') return '深色'
  if (theme.value === 'light') return '浅色'
  return '跟随系统'
})
</script>
