<template>
  <div v-loading="loading">
    <h2 style="margin-bottom:24px">📊 数据概览</h2>
    <el-alert v-if="errMsg" :title="errMsg" type="error" closable @close="errMsg=''" style="margin-bottom:16px" />

    <el-row :gutter="20" style="margin-bottom:24px">
      <el-col :span="6" v-for="card in cards" :key="card.title">
        <el-card shadow="hover"><div style="text-align:center"><div :style="{fontSize:'36px',fontWeight:'700',color:card.color}">{{ card.value }}</div><div style="color:#999;margin-top:8px">{{ card.title }}</div></div></el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-bottom:24px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>路线状态分布</template>
          <div style="display:flex;gap:24px;align-items:center">
            <div style="text-align:center"><div style="font-size:28px;font-weight:700;color:#1976D2">{{ stats.publishedRoutes || 0 }}</div><div style="color:#999;font-size:13px">已发布</div></div>
            <div style="text-align:center"><div style="font-size:28px;font-weight:700;color:#FF9800">{{ stats.recordingRoutes || 0 }}</div><div style="color:#999;font-size:13px">记录中</div></div>
            <div style="text-align:center"><div style="font-size:28px;font-weight:700;color:#999">{{ (stats.routes||0) - (stats.publishedRoutes||0) - (stats.recordingRoutes||0) }}</div><div style="color:#999;font-size:13px">其他</div></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>最近路书</template>
          <div v-for="r in recentRoutes" :key="r.id" style="display:flex;justify-content:space-between;align-items:center;padding:8px 0;border-bottom:1px solid #f5f5f5">
            <div style="flex:1">
              <div style="font-size:14px;font-weight:500;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{r.title}}</div>
              <div style="font-size:12px;color:#999">{{r.totalDays}}天 · {{r.totalDistance||0}}km</div>
            </div>
            <el-tag size="small" :type="r.status===1?'success':r.status===2?'warning':'info'">{{r.status===1?'已发布':r.status===2?'记录中':'草稿'}}</el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'; import api from '../utils/api'
let loading=ref(true), errMsg=ref('')
let cards=ref([{title:'用户数',value:'--',color:'#2E7D32'},{title:'路书数',value:'--',color:'#1976D2'},{title:'模板数',value:'--',color:'#FF9800'},{title:'POI数',value:'--',color:'#9C27B0'}])
let stats=ref({})
let recentRoutes=ref([])
function fmt(d){return d?d.substring(0,10):''}
onMounted(async()=>{
  try{let r=await api.get('/admin/stats');let d=r.data.data||{};stats.value=d;cards.value[0].value=d.users||0;cards.value[1].value=d.routes||0;cards.value[2].value=d.templates||0;cards.value[3].value=d.pois||0}catch(e){errMsg.value='加载统计数据失败'}
  try{let r=await api.get('/routes/community',{params:{size:5}});recentRoutes.value=r.data.data||[]}catch{}
  loading.value=false
})
</script>
