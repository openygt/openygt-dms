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
        <!-- 1. 生产指挥 -->
        <el-sub-menu index="/command">
          <template #title>
            <el-icon><DataLine /></el-icon>
            <span>生产指挥</span>
          </template>
          <el-menu-item v-if="userStore.hasPermission('ops:dashboard:view')" index="/dashboard">生产看板</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('prod:assignment:view')" index="/task-assignment">排产调度</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('prod:monitor:view')" index="/time-monitor">时效监控</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('eq:alarm:view')" index="/alarms">告警管理</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('eq:device:monitor')" index="/device-monitor">设备监控</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('ops:capacity:view')" index="/capacity">产能统计</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('eq:device:efficiency')" index="/device-utilization">设备效能</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('eq:device:control')" index="/device-command">设备操控</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('eq:device:monitor')" index="/digital-twin">数字孪生</el-menu-item>
        </el-sub-menu>

        <!-- 2. 煎药作业 -->
        <el-sub-menu index="/prod">
          <template #title>
            <el-icon><FirstAidKit /></el-icon>
            <span>煎药作业</span>
          </template>
          <el-menu-item v-if="userStore.hasPermission('prod:task:view')" index="/tasks">煎药任务</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('prod:trace:view')" index="/step-visualization">流程跟踪</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('prod:dosing:view')" index="/herb-group">分组投料</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('prod:voice:view')" index="/voice-setting">语音播报</el-menu-item>
        </el-sub-menu>

        <!-- 3. 质量检验 -->
        <el-sub-menu index="/qc">
          <template #title>
            <el-icon><CircleCheck /></el-icon>
            <span>质量检验</span>
          </template>
          <el-menu-item v-if="userStore.hasPermission('qt:inspect:view')" index="/quality">质量检验</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('qt:retain:manage')" index="/retain-sample">留样管理</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('prod:exception:manage')" index="/exception-order">异常工单</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('prod:rework:view')" index="/task-rollback">返工处理</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('eq:temp:view')" index="/temperature-curve">温曲查询</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('qt:yield:view')" index="/report/qc-rate">合格统计</el-menu-item>
        </el-sub-menu>

        <!-- 4. 发药管理 -->
        <el-sub-menu index="/dispatch">
          <template #title>
            <el-icon><Box /></el-icon>
            <span>发药管理</span>
          </template>
          <el-menu-item v-if="userStore.hasPermission('inv:storage:view')" index="/shelf-manage">成品暂存</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('prod:dispatch:view')" index="/prod/delivery">发药确认</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('prod:progress:view')" index="/patient-query">进度查询</el-menu-item>
        </el-sub-menu>

        <!-- 5. 追溯查询 -->
        <el-sub-menu index="/trace">
          <template #title>
            <el-icon><Search /></el-icon>
            <span>追溯查询</span>
          </template>
          <el-menu-item v-if="userStore.hasPermission('trace:rx:view')" index="/traces">处方追溯</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('trace:batch:view')" index="/trace/batch">批次追溯</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('trace:exc:view')" index="/trace/exception">异常追溯</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('sys:log:view')" index="/logs">操作日志</el-menu-item>
        </el-sub-menu>

        <!-- 6. 工艺配置 -->
        <el-sub-menu index="/formula">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>工艺配置</span>
          </template>
          <el-menu-item v-if="userStore.hasPermission('md:scheme:view')" index="/schemes">煎药方案</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('eq:water:view')" index="/water-formulas">加水公式</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('md:package:view')" index="/formula/package-spec">包装规格</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('eq:alarm:view')" index="/alarm-configs">告警配置</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('eq:maint:view')" index="/device-maintenance">设备维保</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('eq:wash:view')" index="/wash-record">清洗记录</el-menu-item>
        </el-sub-menu>

        <!-- 7. 基础数据 -->
        <el-sub-menu index="/base">
          <template #title>
            <el-icon><OfficeBuilding /></el-icon>
            <span>基础数据</span>
          </template>
          <el-menu-item v-if="userStore.hasPermission('md:hospital:view')" index="/hospitals">医院管理</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('md:dept:view')" index="/base/department">科室管理</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('md:doctor:view')" index="/base/doctor">医师管理</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('md:herb:view')" index="/base/medicine">药材管理</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('base:toxic:manage')" index="/toxic-medicine">毒性药材管理</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('sys:user:view')" index="/users">人员管理</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('sys:barcode:view')" index="/employee-barcode">身份条码</el-menu-item>
        </el-sub-menu>

        <!-- 8. 打印中心 -->
        <el-sub-menu index="/print">
          <template #title>
            <el-icon><Printer /></el-icon>
            <span>打印中心</span>
          </template>
          <el-menu-item v-if="userStore.hasPermission('prt:label:view')" index="/print/template">标签打印</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('prt:printer:view')" index="/print/printer">打印管理</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('prt:log:view')" index="/print/log">打印记录</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('prt:workorder:view')" index="/work-order-print">工单打印</el-menu-item>
        </el-sub-menu>

        <!-- 9. 系统管理 -->
        <el-sub-menu index="/system">
          <template #title>
            <el-icon><Lock /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item v-if="userStore.hasPermission('sys:user:view')" index="/sys/users">用户管理</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('sys:role:view')" index="/roles">权限管理</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('sys:menu:view')" index="/menus">菜单管理</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('sys:config:view')" index="/configs">参数配置</el-menu-item>
          <el-menu-item v-if="userStore.hasPermission('sys:log:view')" index="/sys/logs">系统日志</el-menu-item>
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

/* 页面标题样式 */
.page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 16px;
}
.page-header-sub {
  font-weight: normal;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .sidebar {
    display: none;
  }
}
</style>
