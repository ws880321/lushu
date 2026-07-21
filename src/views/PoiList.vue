<template>
  <div>
    <h2>POI 管理</h2>

    <div class="toolbar">
      <el-input
        v-model="search"
        placeholder="搜索 POI..."
        style="width: 200px"
        clearable
        @input="onSearch"
      />
      <el-select
        v-model="categoryFilter"
        placeholder="类别"
        clearable
        style="width: 120px"
        @change="fetchData"
      >
        <el-option label="景点" value="scenic" />
        <el-option label="酒店" value="hotel" />
        <el-option label="露营地" value="camping" />
        <el-option label="加油站" value="gas" />
        <el-option label="充电站" value="charging" />
        <el-option label="停车场" value="parking" />
        <el-option label="餐厅" value="restaurant" />
        <el-option label="卫生间" value="toilet" />
      </el-select>
      <el-select
        v-model="provinceFilter"
        placeholder="省份"
        clearable
        filterable
        style="width: 120px"
        @change="fetchData"
      >
        <el-option
          v-for="p in provinces"
          :key="p"
          :label="p"
          :value="p"
        />
      </el-select>
      <el-button type="primary" @click="$router.push('/pois/new')">
        + 新增 POI
      </el-button>
      <el-button @click="exportCsv">
        📥 导出 CSV
      </el-button>
    </div>

    <el-table :data="pois" stripe v-loading="loading" style="width: 100%">
      <template #empty>
        <el-empty v-if="!loading" :description="search||categoryFilter||provinceFilter?'未找到匹配的 POI':'暂无 POI 数据'" />
      </template>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="category" label="类别" width="80">
        <template #default="{ row }">
          {{ categoryLabel(row.category) }}
        </template>
      </el-table-column>
      <el-table-column prop="driveScore" label="自驾评分" width="90" />
      <el-table-column prop="province" label="省份" width="70" />
      <el-table-column prop="city" label="城市" width="80" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/pois/${row.id}/edit`)">
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

const pois = ref([])
const loading = ref(false)
const search = ref('')
const categoryFilter = ref('')
const provinceFilter = ref('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const provinces = [
  '北京', '天津', '上海', '重庆', '河北', '山西', '辽宁', '吉林',
  '黑龙江', '江苏', '浙江', '安徽', '福建', '江西', '山东', '河南',
  '湖北', '湖南', '广东', '海南', '四川', '贵州', '云南', '陕西',
  '甘肃', '青海', '台湾', '内蒙古', '广西', '西藏', '宁夏', '新疆',
  '香港', '澳门',
]

function categoryLabel(cat) {
  const map = {
    scenic: '景点',
    hotel: '酒店',
    camping: '露营地',
    gas: '加油站',
    charging: '充电站',
    parking: '停车场',
    restaurant: '餐厅',
    toilet: '卫生间',
  }
  return map[cat] || cat
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
    const res = await api.get('/admin/pois', {
      params: {
        page: page.value - 1,
        size: pageSize.value,
        category: categoryFilter.value || null,
        province: provinceFilter.value || null,
        name: search.value || null,
      },
    })
    pois.value = res.data.data.content
    total.value = res.data.data.totalElements
  } catch (e) {
    ElMessage.error('加载 POI 列表失败')
  } finally {
    loading.value = false
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除这个 POI 吗？', '确认', {
      type: 'warning',
    })
    await api.delete(`/admin/pois/${id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(fetchData)

function exportCsv() {
  const params = new URLSearchParams()
  if (categoryFilter.value) params.set('category', categoryFilter.value)
  if (provinceFilter.value) params.set('province', provinceFilter.value)
  if (search.value) params.set('name', search.value)
  window.open('/api/v1/admin/export/pois?' + params.toString(), '_blank')
}
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
