<template>
  <el-dialog v-model="visible" title="修改密码" width="400px" :close-on-click-modal="false">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="旧密码" prop="oldPassword">
        <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入旧密码" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="form.newPassword" type="password" show-password placeholder="6-32位字符" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="submit" :loading="loading">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/api/request'

const visible = ref(false)
const loading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '长度在 6 到 32 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: Function) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

function open() {
  visible.value = true
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await request.post('/v1/sys/users/change-password', {
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    visible.value = false
    // 延迟退出让用户看到提示
    setTimeout(() => {
      location.reload()
    }, 1500)
  } catch (e: any) {
    ElMessage.error(e.message || '修改失败')
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>
