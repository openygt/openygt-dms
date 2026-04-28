<template>
  <div class="app-wrapper" :class="{ 'is-mobile': isMobile }">
    <router-view />
    <MobileNav />
  </div>
</template>

<script setup lang="ts">
import { useDevice } from './composables/useDevice'
import MobileNav from './components/mobile/MobileNav.vue'

const { isMobile } = useDevice()
</script>

<style>
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
}

.app-wrapper.is-mobile {
  padding-bottom: calc(56px + env(safe-area-inset-bottom));
}

/* H5 响应式全局适配 */
@media (max-width: 768px) {
  .el-aside {
    display: none !important;
  }
  
  .el-main {
    margin-left: 0 !important;
    padding: 8px !important;
  }
  
  .el-table {
    display: none;
  }
  
  .mobile-card-list {
    display: block;
  }
  
  .el-form-item {
    margin-bottom: 12px;
  }
  
  .el-button {
    min-height: 44px;
    min-width: 44px;
  }
  
  .el-dialog {
    width: 90% !important;
    margin-top: 10vh !important;
  }
}

@media (min-width: 769px) {
  .mobile-card-list {
    display: none;
  }
}
</style>
