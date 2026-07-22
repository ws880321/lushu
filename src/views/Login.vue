<template>
  <div class="login-page">
    <el-card class="login-card">
      <h1>🚙 路书管理后台</h1>
      <el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon style="margin-bottom:16px" @close="errorMsg=''" />
      <el-button type="success" size="large" @click="login" :loading="loading" style="width:100%">
        管理员登录
      </el-button>
      <p style="color:#999;font-size:12px;margin-top:12px">仅限管理员账号访问</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../utils/api'

const router = useRouter()
const loading = ref(false)
const errorMsg = ref('')

async function login() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await api.post('/auth/wechat-login', {
      code: 'admin_' + Date.now(),
      nickname: '管理员'
    })
    if (res.data.code === 0 && res.data.data?.token) {
      const token = res.data.data.token
      localStorage.setItem('admin_token', token)
      // Verify the token belongs to an admin user
      try {
        const meRes = await api.get('/auth/me', {
          headers: { Authorization: 'Bearer ' + token }
        })
        const user = meRes.data.data
        if (user && user.role === 'admin') {
          router.replace('/dashboard')
        } else {
          localStorage.removeItem('admin_token')
          errorMsg.value = '此账号不是管理员，无法登录管理后台'
        }
      } catch {
        localStorage.removeItem('admin_token')
        errorMsg.value = '登录验证失败，请重试'
      }
    } else {
      errorMsg.value = res.data.message || '登录失败'
    }
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '登录失败，请检查网络'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex; justify-content: center; align-items: center;
  height: 100vh; background: #f0f2f5;
}
.login-card { width: 360px; text-align: center; }
.login-card h1 { font-size: 24px; margin-bottom: 24px; color: #2E7D32; }
</style>
