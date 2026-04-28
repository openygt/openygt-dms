<template>
  <nav class="mobile-nav" v-if="isMobile">
    <router-link 
      v-for="item in navItems" 
      :key="item.path"
      :to="item.path"
      class="nav-item"
      :class="{ active: $route.path === item.path }"
    >
      <el-icon :size="24">
        <component :is="item.icon" />
      </el-icon>
      <span class="nav-text">{{ item.label }}</span>
    </router-link>
  </nav>
</template>

<script setup lang="ts">
import { useDevice } from '../../composables/useDevice'
import {
  HomeFilled,
  List,
  FirstAidKit,
  Setting
} from '@element-plus/icons-vue'

const { isMobile } = useDevice()

const navItems = [
  { path: '/', label: '首页', icon: HomeFilled },
  { path: '/tasks', label: '任务', icon: List },
  { path: '/devices', label: '设备', icon: FirstAidKit },
  { path: '/settings', label: '设置', icon: Setting }
]
</script>

<style scoped>
.mobile-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 56px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-around;
  align-items: center;
  z-index: 1000;
  padding-bottom: env(safe-area-inset-bottom);
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  text-decoration: none;
  font-size: 12px;
  gap: 4px;
  padding: 4px 12px;
}

.nav-item.active {
  color: #409eff;
}

.nav-text {
  font-size: 11px;
}
</style>
