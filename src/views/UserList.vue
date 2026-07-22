<template>
  <div>
    <h2 style="margin-bottom:20px">👤 用户管理</h2>
    <el-table :data="users" v-loading="loading" empty-text="暂无用户数据" style="width:100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{row}">
          <el-tag :type="row.role==='admin'?'danger':'info'" size="small">{{row.role==='admin'?'管理员':'用户'}}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="membership" label="会员" width="80">
        <template #default="{row}">
          <el-tag v-if="row.membership===2" type="warning" size="small">终身</el-tag>
          <el-tag v-else-if="row.membership===1" type="success" size="small">Pro</el-tag>
          <span v-else style="color:#999">免费</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="180">
        <template #default="{row}">{{ fmt(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{row}">
          <el-button v-if="row.role!=='admin'" size="small" type="warning" @click="setRole(row.id,'admin')">设为管理员</el-button>
          <el-button v-else size="small" type="info" @click="setRole(row.id,'user')">取消管理员</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/api'

const users = ref([])
const loading = ref(true)

function fmt(d) { return d ? d.substring(0, 10) : '' }

async function fetch() {
  loading.value = true
  try {
    const r = await api.get('/admin/users')
    users.value = r.data.data || []
  } catch {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

async function setRole(id, role) {
  const label = role === 'admin' ? '设为管理员' : '取消管理员'
  try {
    await ElMessageBox.confirm('确定要' + label + '吗？', '确认操作', { type: 'warning' })
    await api.put('/admin/users/' + id + '/role', { role })
    ElMessage.success('角色已更新')
    fetch()
  } catch { /* cancelled */ }
}

onMounted(fetch)
</script>
