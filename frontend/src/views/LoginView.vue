<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="login-title">智能煎药管理系统</h1>
      <p class="login-subtitle">OpenYGT · 开源医共体</p>
      <el-form :model="form" @keyup.enter="handleLogin" ref="formRef">
        <el-form-item>
          <el-input 
            v-model="form.username" 
            placeholder="用户名"
            size="large"
            :prefix-icon="User" 
          />
        </el-form-item>
        <el-form-item>
          <el-input 
            v-model="form.password" 
            type="password"
            placeholder="密码"
            size="large"
            show-password
            :prefix-icon="Lock" 
          />
        </el-form-item>
        <el-button 
          :loading="loading" 
          type="primary"
          size="large"
          style="width: 100%; margin-top: 8px"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>
      <div class="login-footer">
        <span class="version-text">v1.0.0</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    // 错误已在拦截器中提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  
  /* 新背景:深蓝单色 + 草本绿点缀 */
  background: 
    radial-gradient(ellipse at top right, var(--ygt-herb-700) 0%, transparent 50%),
    radial-gradient(ellipse at bottom left, var(--ygt-primary-700) 0%, transparent 50%),
    var(--ygt-primary-900);
}

.login-card {
  width: 420px;
  padding: var(--ygt-space-8);
  background: var(--ygt-bg-surface);
  border-radius: var(--ygt-radius-lg);
  box-shadow: var(--ygt-shadow-lg);
}

.login-title {
  font-size: var(--ygt-text-2xl);
  font-weight: var(--ygt-fw-semibold);
  color: var(--ygt-text-primary);
  text-align: center;
  margin-bottom: var(--ygt-space-2);
}

.login-subtitle {
  font-size: var(--ygt-text-sm);
  color: var(--ygt-text-tertiary);
  text-align: center;
  margin-bottom: var(--ygt-space-8);
}

.login-footer {
  margin-top: var(--ygt-space-8);
  text-align: center;
  font-size: var(--ygt-text-xs);
  color: var(--ygt-text-tertiary);
}

.login-footer a {
  color: var(--ygt-primary-500);
}

.login-footer a:hover {
  color: var(--ygt-primary-600);
}

@media (max-width: 768px) {
  .login-card {
    width: 90%;
    padding: var(--ygt-space-6);
  }
}
</style>
