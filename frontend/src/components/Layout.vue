<template>
  <el-container class="layout-container">
    <el-aside class="sidebar" width="var(--ygt-sidebar-width)">
      <div class="sidebar-logo">
        <el-icon :size="24" color="#fff"><FirstAidKit /></el-icon>
        <span>智能煎药管理系统</span>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        class="sidebar-menu"
        text-color="#F4F4F5"
        active-text-color="#FFFFFF"
        background-color="transparent"
        style="border-right: none"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>
          <span>实时看板</span>
        </el-menu-item>
        <el-sub-menu index="/prod">
          <template #title>
            <el-icon><List /></el-icon>
            <span>生产管理</span>
          </template>
          <el-menu-item index="/tasks">任务管理</el-menu-item>
          <el-menu-item index="/prescriptions">处方管理</el-menu-item>
          <el-menu-item index="/consume-log">药材消耗</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/eq">
          <template #title>
            <el-icon><Cpu /></el-icon>
            <span>设备监控</span>
          </template>
          <el-menu-item index="/devices">设备管理</el-menu-item>
          <el-menu-item index="/alarms">告警日志</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/print-center">
          <el-icon><Printer /></el-icon>
          <span>打印管理</span>
        </el-menu-item>
        <el-menu-item index="/quality">
          <el-icon><CircleCheck /></el-icon>
          <span>质检记录</span>
        </el-menu-item>
        <el-menu-item index="/capacity">
          <el-icon><TrendCharts /></el-icon>
          <span>产能报表</span>
        </el-menu-item>
        <el-sub-menu index="/md">
          <template #title>
            <el-icon><OfficeBuilding /></el-icon>
            <span>基础数据</span>
          </template>
          <el-menu-item index="/hospitals">医院管理</el-menu-item>
          <el-menu-item index="/schemes">煎煮方案</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/users">用户管理</el-menu-item>
          <el-menu-item index="/roles">角色管理</el-menu-item>
          <el-menu-item index="/menus">菜单管理</el-menu-item>
          <el-menu-item index="/configs">系统配置</el-menu-item>
          <el-menu-item index="/logs">系统日志</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <span class="header-title">{{ $route.meta.title || 'OpenYGT' }}</span>
        <div class="header-actions">
          <ThemeToggle />
          <el-divider direction="vertical" />
          <span class="header-user">当前用户：{{ userStore.userInfo?.username || '未登录' }}</span>
          <el-button size="small" @click="userStore.logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import ThemeToggle from './ThemeToggle.vue'
const userStore = useUserStore()
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background: var(--ygt-primary-900);
  color: var(--ygt-gray-100);
  border-right: 1px solid var(--ygt-primary-800);
}

.sidebar-logo {
  height: var(--ygt-header-height);
  display: flex;
  align-items: center;
  gap: var(--ygt-space-2);
  padding: 0 var(--ygt-space-4);
  background: #001626;
  border-bottom: 1px solid var(--ygt-primary-800);
  font-weight: var(--ygt-fw-semibold);
  color: #fff;
  font-size: var(--ygt-text-lg);
}

.sidebar-menu {
  background: transparent !important;
  border-right: none !important;
}

/* 菜单项统一样式 */
.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 44px;
  margin: 2px 8px;
  border-radius: var(--ygt-radius-md);
  color: #F4F4F5 !important;
  background: transparent !important;
  transition: all var(--ygt-duration-fast) var(--ygt-ease-out);
}

/* 展开的子菜单（inline）背景必须是深蓝 */
.sidebar-menu :deep(.el-menu--inline) {
  background: transparent !important;
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: var(--ygt-primary-800) !important;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: var(--ygt-primary-500) !important;
  color: #fff !important;
  font-weight: var(--ygt-fw-medium);
}

/* 确保子菜单标题和图标颜色一致 */
.sidebar-menu :deep(.el-sub-menu__title) {
  color: #F4F4F5 !important;
}

.sidebar-menu :deep(.el-sub-menu__title i) {
  color: #F4F4F5 !important;
}

/* 去掉 EP 默认的白色背景 */
.sidebar-menu :deep(.el-menu) {
  background: transparent !important;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--ygt-bg-surface);
  box-shadow: var(--ygt-shadow-sm);
  border-bottom: 1px solid var(--ygt-border);
}

.header-title {
  font-weight: var(--ygt-fw-semibold);
  font-size: var(--ygt-text-md);
  color: var(--ygt-text-primary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--ygt-space-3);
}
.header-actions .el-divider--vertical {
  margin: 0;
}

.header-user {
  color: var(--ygt-text-secondary);
  font-size: var(--ygt-text-sm);
}

.layout-main {
  background: var(--ygt-bg-page);
  padding: var(--ygt-space-4);
}

/* 移动端适配 */
@media (max-width: 768px) {
  .sidebar {
    display: none;
  }
}
</style>
