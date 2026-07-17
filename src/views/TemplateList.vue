<template>
  <div>
    <h2>模板管理</h2>

    <div class="toolbar">
      <el-input
        v-model="regionSearch"
        placeholder="搜索区域..."
        style="width: 200px"
        clearable
        @input="onSearch"
      />
      <el-button type="primary" @click="$router.push('/templates/new')">
        + 新增模板
      </el-button>
    </div>

    <el-table :data="templates" stripe v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="region" label="区域" width="100" />
      <el-table-column prop="totalDays" label="天数" width="60" />
      <el-table-column prop="difficulty" label="难度" width="80">
        <template #default="{ row }">
          {{ difficultyLabel(row.difficulty) }}
        </template>
      </el-table-column>
      <el-table-column prop="usageCount" label="使用次数" width="90" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/templates/${row.id}/edit`)">
            编辑
          </el-button>
          <el-button
            size="small"
            type="danger"
            @click="handleDelete(row.id)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchData"
        @size-change="fetchData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../utils/api.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const templates = ref([])
const loading = ref(false)
const regionSearch = ref('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

function difficultyLabel(d) {
  const map = { 1: '简单', 2: '中等', 3: '困难' }
  return map[d] || '未知'
}

let searchTimer = null
function onSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    page.value = 1
    fetchData()
  }, 300)
}

async function fetchData() {
  loading.value = true
  try {
    const res = await api.get('/admin/templates', {
      params: {
        page: page.value - 1,
        size: pageSize.value,
        region: regionSearch.value || null,
      },
    })
    templates.value = res.data.data.content
    total.value = res.data.data.totalElements
  } catch (e) {
    ElMessage.error('加载模板列表失败')
  } finally {
    loading.value = false
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除这个模板吗？所有途经点也将被删除。', '确认', {
      type: 'warning',
    })
    await api.delete(`/admin/templates/${id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(fetchData)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
