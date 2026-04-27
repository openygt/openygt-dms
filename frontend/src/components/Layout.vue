<template>
  <el-container style="height: 100vh">
    <el-aside width="200px" style="background: #001529; color: #fff">
      <div style="padding: 16px; font-size: 18px; font-weight: bold; text-align: center">
        OpenYGT DMS
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#001529"
        text-color="#fff"
        active-text-color="#409EFF"
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
        </el-sub-menu>
        <el-menu-item index="/consume-log">
          <el-icon><Document /></el-icon>
          <span>消耗流水</span>
        </el-menu-item>
        <el-sub-menu index="/eq">
          <template #title>
            <el-icon><Cpu /></el-icon>
            <span>设备监控</span>
          </template>
          <el-menu-item index="/devices">设备管理</el-menu-item>
          <el-menu-item index="/alarms">告警日志</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/capacity">
          <el-icon><TrendCharts /></el-icon>
          <span>产能报表</span>
        </el-menu-item>
        <el-sub-menu index="/system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/users">用户管理</el-menu-item>
          <el-menu-item index="/roles">角色管理</el-menu-item>
          <el-menu-item index="/menus">菜单管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="display: flex; align-items: center; justify-content: space-between; background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,0.1)">
        <span style="font-weight: bold">{{ $route.meta.title || 'OpenYGT' }}</span>
        <div>
          <span style="margin-right: 16px">{{ userStore.userInfo?.username || '未登录' }}</span>
          <el-button size="small" @click="userStore.logout">退出</el-button>
        </div>
      </el-header>
      <el-main style="background: #f0f2f5; padding: 16px">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'
const userStore = useUserStore()
</script>
