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
            <el-icon><FirstAidKit /></el-icon>
            <span>生产执行</span>
          </template>
          <el-menu-item index="/prod/receive">处方接收</el-menu-item>
          <el-menu-item index="/tasks">煎煮任务</el-menu-item>
          <el-menu-item index="/quality">质检管理</el-menu-item>
          <el-menu-item index="/prod/delivery">交付管理</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/eq">
          <template #title>
            <el-icon><Cpu /></el-icon>
            <span>设备管理</span>
          </template>
          <el-menu-item index="/device-monitor">设备监控</el-menu-item>
          <el-menu-item index="/devices">设备台账</el-menu-item>
          <el-menu-item index="/alarms">告警日志</el-menu-item>
          <el-menu-item index="/alarm-configs">告警配置</el-menu-item>
          <el-menu-item index="/temperature-curve">温度曲线</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/formula">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>工艺配置</span>
          </template>
          <el-menu-item index="/schemes">煎煮方案</el-menu-item>
          <el-menu-item index="/water-formulas">加水量公式</el-menu-item>
          <el-menu-item index="/prescription-defaults">处方默认设置</el-menu-item>
          <el-menu-item index="/formula/package-spec">包装规格</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/trace">
          <template #title>
            <el-icon><Search /></el-icon>
            <span>质量追溯</span>
          </template>
          <el-menu-item index="/traces">处方追溯</el-menu-item>
          <el-menu-item index="/trace/batch">批次追溯</el-menu-item>
          <el-menu-item index="/trace/exception">异常追溯</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/report">
          <template #title>
            <el-icon><TrendCharts /></el-icon>
            <span>数据分析</span>
          </template>
          <el-menu-item index="/eq-dashboard">数据看板</el-menu-item>
          <el-menu-item index="/capacity">产能报表</el-menu-item>
          <el-menu-item index="/workload">工作量统计</el-menu-item>
          <el-menu-item index="/device-utilization">设备利用率</el-menu-item>
          <el-menu-item index="/consume-log">药材消耗</el-menu-item>
          <el-menu-item index="/report/qc-rate">质检合格率</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/print">
          <template #title>
            <el-icon><Printer /></el-icon>
            <span>打印配置</span>
          </template>
          <el-menu-item index="/print/template">标签模板</el-menu-item>
          <el-menu-item index="/print/printer">打印机管理</el-menu-item>
          <el-menu-item index="/print/log">打印记录</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/base">
          <template #title>
            <el-icon><OfficeBuilding /></el-icon>
            <span>基础数据</span>
          </template>
          <el-menu-item index="/hospitals">医院管理</el-menu-item>
          <el-menu-item index="/base/department">科室管理</el-menu-item>
          <el-menu-item index="/base/doctor">医师管理</el-menu-item>
          <el-menu-item index="/base/medicine">药材目录</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/system">
          <template #title>
            <el-icon><Lock /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/users">用户管理</el-menu-item>
          <el-menu-item index="/roles">角色权限</el-menu-item>
          <el-menu-item index="/menus">菜单管理</el-menu-item>
          <el-menu-item index="/logs">操作审计</el-menu-item>
          <el-menu-item index="/configs">系统参数</el-menu-item>
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
          <el-button size="small" @click="openChangePassword">修改密码</el-button>
          <el-button size="small" @click="userStore.logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
      <ChangePasswordDialog ref="changePasswordRef" />
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import ThemeToggle from './ThemeToggle.vue'
import ChangePasswordDialog from './ChangePasswordDialog.vue'
import { ref } from 'vue'

const userStore = useUserStore()
const changePasswordRef = ref<InstanceType<typeof ChangePasswordDialog>>()

function openChangePassword() {
  changePasswordRef.value?.open()
}
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
