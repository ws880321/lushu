<template>
  <div>
    <h2>{{ isEdit ? '编辑模板' : '新建模板' }}</h2>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      style="max-width: 720px"
      @submit.prevent="handleSave"
    >
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" placeholder="模板名称" />
      </el-form-item>

      <el-form-item label="区域" prop="region">
        <el-input v-model="form.region" placeholder="如：新疆" />
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="天数" prop="totalDays">
            <el-input-number v-model="form.totalDays" :min="1" :max="15" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="总距离(km)">
            <el-input-number v-model="form.totalDistance" :min="0" :step="10" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="难度">
            <el-select v-model="form.difficulty" style="width: 100px">
              <el-option label="简单" :value="1" />
              <el-option label="中等" :value="2" />
              <el-option label="困难" :value="3" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="最佳季节">
        <el-input v-model="form.bestSeason" placeholder="如：6-9月" />
      </el-form-item>

      <el-form-item label="标签">
        <el-select
          v-model="tags"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="输入标签后回车"
          style="width: 100%"
        >
          <el-option
            v-for="tag in commonTags"
            :key="tag"
            :label="tag"
            :value="tag"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="封面图">
        <el-input v-model="form.coverImage" placeholder="图片URL" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
        <el-button @click="$router.push('/templates')">取消</el-button>
      </el-form-item>
    </el-form>

    <!-- Waypoints section (only visible when editing) -->
    <template v-if="isEdit && templateId">
      <el-divider>途经点管理</el-divider>

      <el-table :data="waypoints" row-key="id" stripe v-loading="wpLoading" style="width: 100%">
        <el-table-column prop="dayNumber" label="Day" width="60" />
        <el-table-column prop="sortOrder" label="顺序" width="60" />
        <el-table-column prop="pointType" label="类型" width="80">
          <template #default="{ row }">
            {{ pointTypeLabel(row.pointType) }}
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column label="坐标" width="200">
          <template #default="{ row }">
            <span v-if="row.lng">{{ row.lng }}, {{ row.lat }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stayDuration" label="停留(min)" width="100" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="editWp(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="delWp(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-button type="primary" style="margin-top: 12px" @click="openWpDialog()">
        + 添加途经点
      </el-button>
    </template>

    <!-- Waypoint edit dialog -->
    <el-dialog
      v-model="wpDialogVisible"
      :title="wpEditingId ? '编辑途经点' : '添加途经点'"
      width="600px"
    >
      <el-form :model="wpForm" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="天数">
              <el-input-number v-model="wpForm.dayNumber" :min="1" :max="15" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="顺序">
              <el-input-number v-model="wpForm.sortOrder" :min="1" :max="999" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类型">
              <el-select v-model="wpForm.pointType" style="width: 100%">
                <el-option label="景点" value="scenic" />
                <el-option label="餐饮" value="food" />
                <el-option label="酒店" value="hotel" />
                <el-option label="加油站" value="gas" />
                <el-option label="充电站" value="charging" />
                <el-option label="停车场" value="parking" />
                <el-option label="拍照点" value="photo" />
                <el-option label="自定义" value="custom" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="停留(min)">
              <el-input-number v-model="wpForm.stayDuration" :min="0" :max="1440" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="名称">
          <el-input v-model="wpForm.name" placeholder="途经点名称" />
        </el-form-item>

        <el-form-item label="坐标">
          <MapPicker v-model="wpCoords" />
          <el-tag v-if="wpForm.lng" style="margin-top: 4px">
            {{ wpForm.lng }}, {{ wpForm.lat }}
          </el-tag>
        </el-form-item>

        <el-form-item label="自驾贴士">
          <el-input v-model="wpForm.tips" type="textarea" :rows="3" placeholder="自驾贴士" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="wpDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="wpSaving" @click="saveWp">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../utils/api.js'
import { ElMessage } from 'element-plus'
import MapPicker from '../components/MapPicker.vue'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const saving = ref(false)

const isEdit = computed(() => route.name === 'TemplateEdit')
const templateId = computed(() => route.params.id)

const commonTags = [
  '摄影', '雪山', '草原', '沙漠', '海岸', '峡谷', '森林',
  '自驾', '房车', '露营', '亲子', '古村', '古道',
]

const form = ref({
  name: '',
  region: '',
  totalDays: 3,
  totalDistance: null,
  bestSeason: '',
  difficulty: 2,
  tags: '',
  coverImage: '',
})

const tags = ref([])

// Sync tags array <-> form.tags JSON string
watch(tags, (val) => {
  form.value.tags = JSON.stringify(val)
}, { deep: true })

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  region: [{ required: true, message: '请输入区域', trigger: 'blur' }],
  totalDays: [{ required: true, message: '请输入天数', trigger: 'blur' }],
}

// ---- Waypoints ----

const waypoints = ref([])
const wpLoading = ref(false)
const wpDialogVisible = ref(false)
const wpSaving = ref(false)
const wpEditingId = ref(null)

const wpForm = ref({})
const wpCoords = ref(null)

watch(wpCoords, (val) => {
  if (val) {
    wpForm.value.lng = val.lng
    wpForm.value.lat = val.lat
  } else {
    wpForm.value.lng = null
    wpForm.value.lat = null
  }
})

function pointTypeLabel(t) {
  const map = {
    scenic: '景点', food: '餐饮', hotel: '酒店', gas: '加油站',
    charging: '充电站', parking: '停车场', photo: '拍照点', custom: '自定义',
  }
  return map[t] || t
}

function openWpDialog(wp) {
  if (wp) {
    wpEditingId.value = wp.id
    wpForm.value = { ...wp }
    if (wp.lng && wp.lat) {
      wpCoords.value = { lng: wp.lng, lat: wp.lat }
    } else {
      wpCoords.value = null
    }
  } else {
    wpEditingId.value = null
    wpForm.value = {
      dayNumber: 1,
      sortOrder: waypoints.value.length + 1,
      pointType: 'scenic',
      name: '',
      lng: null,
      lat: null,
      stayDuration: 30,
      tips: '',
    }
    wpCoords.value = null
  }
  wpDialogVisible.value = true
}

function editWp(row) {
  openWpDialog(row)
}

async function saveWp() {
  wpSaving.value = true
  try {
    const payload = {
      dayNumber: wpForm.value.dayNumber,
      sortOrder: wpForm.value.sortOrder,
      pointType: wpForm.value.pointType,
      name: wpForm.value.name,
      lng: wpForm.value.lng,
      lat: wpForm.value.lat,
      stayDuration: wpForm.value.stayDuration,
      tips: wpForm.value.tips || null,
    }

    if (wpEditingId.value) {
      await api.put(`/admin/templates/${templateId.value}/waypoints/${wpEditingId.value}`, payload)
      ElMessage.success('途经点更新成功')
    } else {
      await api.post(`/admin/templates/${templateId.value}/waypoints`, payload)
      ElMessage.success('途经点添加成功')
    }
    wpDialogVisible.value = false
    loadWaypoints()
  } catch (e) {
    ElMessage.error('保存途经点失败')
  } finally {
    wpSaving.value = false
  }
}

async function delWp(wpId) {
  try {
    const { ElMessageBox } = await import('element-plus')
    await ElMessageBox.confirm('确定删除这个途经点吗？', '确认', { type: 'warning' })
    await api.delete(`/admin/templates/${templateId.value}/waypoints/${wpId}`)
    ElMessage.success('删除成功')
    loadWaypoints()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

async function loadWaypoints() {
  if (!templateId.value) return
  wpLoading.value = true
  try {
    const res = await api.get(`/admin/templates/${templateId.value}`)
    const data = res.data.data
    if (data.waypoints) {
      waypoints.value = data.waypoints
    }
    if (data.template) {
      const tpl = data.template
      // If form is still empty (first load), populate from response
      if (form.value.name === '' && tpl.name) {
        form.value.name = tpl.name
        form.value.region = tpl.region
        form.value.totalDays = tpl.totalDays
        form.value.totalDistance = tpl.totalDistance
        form.value.bestSeason = tpl.bestSeason || ''
        form.value.difficulty = tpl.difficulty || 2
        form.value.coverImage = tpl.coverImage || ''
        try {
          tags.value = tpl.tags ? JSON.parse(tpl.tags) : []
        } catch {
          tags.value = []
        }
      }
    }
  } catch (e) {
    ElMessage.error('加载途经点失败')
  } finally {
    wpLoading.value = false
  }
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = {
      name: form.value.name,
      region: form.value.region,
      totalDays: form.value.totalDays,
      totalDistance: form.value.totalDistance || null,
      bestSeason: form.value.bestSeason || null,
      difficulty: form.value.difficulty,
      tags: form.value.tags || null,
      coverImage: form.value.coverImage || null,
    }

    if (isEdit.value) {
      await api.put(`/admin/templates/${templateId.value}`, payload)
      ElMessage.success('保存成功')
    } else {
      const res = await api.post('/admin/templates', payload)
      // Redirect to edit page for the newly created template
      const newId = res.data.data.id
      ElMessage.success('创建成功')
      router.replace(`/templates/${newId}/edit`)
      return
    }
    router.push('/templates')
  } catch (e) {
    ElMessage.error(isEdit.value ? '保存失败' : '创建失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  if (isEdit.value && templateId.value) {
    await loadWaypoints()
  }
})
</script>

<style scoped>
</style>
