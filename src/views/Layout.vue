<template>
  <el-container style="height: 100vh">
    <el-aside width="200px" style="background-color: #304156;display:flex;flex-direction:column">
      <div class="logo" style="height:60px;display:flex;align-items:center;justify-content:center;color:#fff;font-size:18px;font-weight:bold;border-bottom:1px solid rgba(255,255,255,0.1)">路书管理</div>
      <el-menu :default-active="activeMenu" background-color="#304156" text-color="#bfcbd9" active-text-color="#409eff" router>
        <el-menu-item index="/dashboard"><el-icon><DataBoard /></el-icon><span>数据概览</span></el-menu-item>
        <el-menu-item index="/users"><el-icon><User /></el-icon><span>用户管理</span></el-menu-item>
        <el-menu-item index="/routes"><el-icon><List /></el-icon><span>路线管理</span></el-menu-item>
        <el-menu-item index="/templates"><el-icon><Document /></el-icon><span>路线模板</span></el-menu-item>
        <el-menu-item index="/pois"><el-icon><Location /></el-icon><span>POI 管理</span></el-menu-item>
      </el-menu>
      <div style="flex:1"></div>
      <div style="padding:12px;border-top:1px solid rgba(255,255,255,0.1);color:#bfcbd9;font-size:13px">
        <div style="margin-bottom:6px">👤 管理员</div>
        <el-button size="small" text style="color:#bfcbd9;width:100%" @click="handleLogout">退出登录</el-button>
      </div>
    </el-aside>
    <el-main><router-view /></el-main>
  </el-container>
</template>
<script setup>
import { computed } from 'vue'; import { useRoute, useRouter } from 'vue-router'
import { DataBoard, Document, Location, User, List } from '@element-plus/icons-vue'
const route = useRoute(); const router = useRouter()
const activeMenu = computed(() => {
  const p = route.path
  if (p.startsWith('/users')) return '/users'
  if (p.startsWith('/routes')) return '/routes'
  if (p.startsWith('/templates')) return '/templates'
  if (p.startsWith('/pois')) return '/pois'
  return '/dashboard'
})
function handleLogout() { localStorage.removeItem('admin_token'); router.replace('/login') }
</script>
