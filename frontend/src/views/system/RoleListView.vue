<template>
  <div>
    <el-card>
      <template #header>
        <span>角色管理</span>
      </template>
      <p>角色列表（占位页面）</p>
      <el-tag v-for="role in roles" :key="role" style="margin-right: 8px">{{ role }}</el-tag>
    </el-card>
  </div>
</template>

<script setup lang="ts">
const roles = ['系统管理员', '主任', '班长', '煎药工', '质检员']
</script>
