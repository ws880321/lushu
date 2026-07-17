<template>
  <div>
    <h2 style="margin-bottom:24px">📊 数据概览</h2>
    <el-row :gutter="20">
      <el-col :span="6" v-for="card in cards" :key="card.title">
        <el-card shadow="hover">
          <div style="text-align:center">
            <div style="font-size:36px;font-weight:700;color:#2E7D32">{{ card.value }}</div>
            <div style="color:#999;margin-top:8px">{{ card.title }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../utils/api'

const cards = ref([
  { title: '用户数', value: '--' },
  { title: '路书数', value: '--' },
  { title: '模板数', value: '--' },
  { title: 'POI数', value: '--' },
])

onMounted(async () => {
  try {
    const data = await api.get('/admin/stats')
    cards.value[0].value = data.users
    cards.value[1].value = data.routes
    cards.value[2].value = data.templates
    cards.value[3].value = data.pois
  } catch (e) {}
})
</script>
