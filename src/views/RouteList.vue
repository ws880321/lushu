<template>
  <div>
    <h2 style="margin-bottom:20px">📋 路线管理</h2>
    <el-table :data="routes" v-loading="loading" empty-text="暂无路线数据" style="width:100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="totalDays" label="天数" width="70" />
      <el-table-column prop="totalDistance" label="距离(km)" width="100" />
      <el-table-column label="状态" width="90">
        <template #default="{row}">
          <el-tag v-if="row.status===1" type="success" size="small">已发布</el-tag>
          <el-tag v-else-if="row.status===2" type="warning" size="small">记录中</el-tag>
          <el-tag v-else type="info" size="small">草稿</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160">
        <template #default="{row}">{{ fmt(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{row}">
          <el-button size="small" type="danger" @click="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/api'

const routes = ref([])
const loading = ref(true)

function fmt(d) { return d ? d.substring(0, 10) : '' }

async function fetch() {
  loading.value = true
  try {
    const resp = await api.get('/routes/community', { params: { size: 500 } })
    routes.value = resp.data.data || []
  } catch {
    ElMessage.error('加载路线列表失败')
  } finally {
    loading.value = false
  }
}

async function doDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除路线"' + row.title + '"吗？此操作不可恢复', '确认删除', { type: 'warning' })
    await api.delete('/routes/' + row.id)
    ElMessage.success('已删除')
    fetch()
  } catch { /* cancelled */ }
}

onMounted(fetch)
</script>
