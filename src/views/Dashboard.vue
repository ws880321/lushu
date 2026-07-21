<template>
  <div v-loading="loading">
    <h2 style="margin-bottom:24px">📊 数据概览</h2>
    <el-alert v-if="errMsg" :title="errMsg" type="error" closable @close="errMsg=''" style="margin-bottom:16px" />
    <el-row :gutter="20" style="margin-bottom:24px">
      <el-col :span="6" v-for="card in cards" :key="card.title">
        <el-card shadow="hover"><div style="text-align:center"><div :style="{fontSize:'36px',fontWeight:'700',color:card.color}">{{ card.value }}</div><div style="color:#999;margin-top:8px">{{ card.title }}</div></div></el-card>
      </el-col>
    </el-row>
    <h3 style="margin-bottom:16px">📋 最近路书</h3>
    <el-table :data="recentRoutes" empty-text="暂无数据" style="width:100%">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="totalDays" label="天数" width="80" />
      <el-table-column prop="totalDistance" label="距离(km)" width="120" />
      <el-table-column prop="createdAt" label="创建时间" width="180"><template #default="{row}">{{ fmt(row.createdAt) }}</template></el-table-column>
    </el-table>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'; import api from '../utils/api'
let loading=ref(true), errMsg=ref('')
let cards=ref([{title:'用户数',value:'--',color:'#2E7D32'},{title:'路书数',value:'--',color:'#1976D2'},{title:'模板数',value:'--',color:'#FF9800'},{title:'POI数',value:'--',color:'#9C27B0'}])
let recentRoutes=ref([])
function fmt(d){return d?d.substring(0,10):''}
onMounted(async()=>{
  try{let r=await api.get('/admin/stats');let d=r.data.data||{};cards.value[0].value=d.users||0;cards.value[1].value=d.routes||0;cards.value[2].value=d.templates||0;cards.value[3].value=d.pois||0}catch(e){errMsg.value='加载统计数据失败'}
  try{let r=await api.get('/routes/community',{params:{size:5}});recentRoutes.value=r.data.data||[]}catch{}
  loading.value=false
})
</script>
