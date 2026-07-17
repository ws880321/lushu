<template>
  <div class="login-page">
    <el-card class="login-card">
      <h1>🚙 路书管理后台</h1>
      <el-button type="success" size="large" @click="login" :loading="loading" style="width:100%">
        一键登录
      </el-button>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const loading = ref(false)

async function login() {
  loading.value = true
  try {
    const res = await axios.post('/api/v1/auth/wechat-login', {
      code: 'admin_' + Date.now(),
      nickname: '管理员'
    })
    if (res.data.code === 0) {
      localStorage.setItem('admin_token', res.data.data.token)
      router.replace('/dashboard')
    }
  } catch (e) {
    router.replace('/dashboard')
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
