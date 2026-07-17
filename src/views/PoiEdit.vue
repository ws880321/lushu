<template>
  <div>
    <h2>{{ isNew ? '新增 POI' : '编辑 POI' }}</h2>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      style="max-width: 720px"
      @submit.prevent="handleSave"
    >
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" placeholder="POI 名称" />
      </el-form-item>

      <el-form-item label="类别" prop="category">
        <el-select v-model="form.category" placeholder="选择类别" style="width: 200px">
          <el-option label="景点" value="scenic" />
          <el-option label="酒店" value="hotel" />
          <el-option label="露营地" value="camping" />
          <el-option label="加油站" value="gas" />
          <el-option label="充电站" value="charging" />
          <el-option label="停车场" value="parking" />
          <el-option label="餐厅" value="restaurant" />
          <el-option label="卫生间" value="toilet" />
        </el-select>
      </el-form-item>

      <el-form-item label="坐标" prop="coords">
        <MapPicker v-model="coords" />
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="省份">
            <el-select v-model="form.province" placeholder="省份" filterable clearable>
              <el-option
                v-for="p in provinces"
                :key="p"
                :label="p"
                :value="p"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="城市">
            <el-input v-model="form.city" placeholder="城市" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="区县">
            <el-input v-model="form.district" placeholder="区县" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="地址">
        <el-input v-model="form.address" placeholder="详细地址" />
      </el-form-item>

      <el-form-item label="电话">
        <el-input v-model="form.phone" placeholder="联系电话" />
      </el-form-item>

      <el-divider>自驾友好度评分</el-divider>

      <el-form-item label="总体评分">
        <ScoreSlider v-model="form.driveScore" />
      </el-form-item>

      <el-form-item label="停车评分">
        <ScoreSlider v-model="form.parkingScore" />
      </el-form-item>

      <el-form-item label="路况评分">
        <ScoreSlider v-model="form.roadScore" />
      </el-form-item>

      <el-divider>其他属性</el-divider>

      <el-form-item label="房车友好">
        <el-select v-model="form.rvFriendly" placeholder="选择" style="width: 200px" clearable>
          <el-option label="否" :value="0" />
          <el-option label="可停车" :value="1" />
          <el-option label="可露营" :value="2" />
        </el-select>
      </el-form-item>

      <el-form-item label="信号质量">
        <el-select v-model="form.signalQuality" placeholder="选择" style="width: 200px" clearable>
          <el-option label="弱" :value="1" />
          <el-option label="中" :value="2" />
          <el-option label="强" :value="3" />
        </el-select>
      </el-form-item>

      <el-form-item label="宠物友好">
        <el-switch v-model="form.petFriendly" :active-value="1" :inactive-value="0" />
      </el-form-item>

      <el-form-item label="允许露营">
        <el-switch v-model="form.campingAllowed" :active-value="1" :inactive-value="0" />
      </el-form-item>

      <el-form-item label="自驾贴士">
        <el-input
          v-model="form.tips"
          type="textarea"
          :rows="4"
          placeholder="自驾贴士（暂未使用，预留字段）"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ isNew ? '创建' : '保存' }}
        </el-button>
        <el-button @click="$router.push('/pois')">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../utils/api.js'
import { ElMessage } from 'element-plus'
import MapPicker from '../components/MapPicker.vue'
import ScoreSlider from '../components/ScoreSlider.vue'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const saving = ref(false)

const isNew = computed(() => route.name === 'PoiCreate')

const provinces = [
  '北京', '天津', '上海', '重庆', '河北', '山西', '辽宁', '吉林',
  '黑龙江', '江苏', '浙江', '安徽', '福建', '江西', '山东', '河南',
  '湖北', '湖南', '广东', '海南', '四川', '贵州', '云南', '陕西',
  '甘肃', '青海', '台湾', '内蒙古', '广西', '西藏', '宁夏', '新疆',
  '香港', '澳门',
]

const form = ref({
  name: '',
  category: '',
  lng: null,
  lat: null,
  province: '',
  city: '',
  district: '',
  address: '',
  phone: '',
  driveScore: 3.0,
  parkingScore: 3.0,
  roadScore: 3.0,
  rvFriendly: null,
  signalQuality: null,
  petFriendly: 0,
  campingAllowed: 0,
  tips: '',
})

const coords = ref(null)

// Sync coords <-> form.lng/lat
watch(coords, (val) => {
  if (val) {
    form.value.lng = val.lng
    form.value.lat = val.lat
  } else {
    form.value.lng = null
    form.value.lat = null
  }
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择类别', trigger: 'change' }],
  coords: [
    {
      validator: () => coords.value !== null || isNew.value === false,
      message: '请选择坐标',
      trigger: 'change',
    },
  ],
}

async function fetchDetail(id) {
  try {
    const res = await api.get(`/admin/pois/${id}`)
    const data = res.data.data
    form.value = {
      name: data.name || '',
      category: data.category || '',
      lng: data.lng,
      lat: data.lat,
      province: data.province || '',
      city: data.city || '',
      district: data.district || '',
      address: data.address || '',
      phone: data.phone || '',
      driveScore: data.driveScore || 3.0,
      parkingScore: data.parkingScore || 3.0,
      roadScore: data.roadScore || 3.0,
      rvFriendly: data.rvFriendly,
      signalQuality: data.signalQuality,
      petFriendly: data.petFriendly != null ? data.petFriendly : 0,
      campingAllowed: data.campingAllowed != null ? data.campingAllowed : 0,
      tips: '',
    }
    if (data.lng && data.lat) {
      coords.value = { lng: data.lng, lat: data.lat }
    }
  } catch (e) {
    ElMessage.error('加载 POI 详情失败')
  }
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = {
      name: form.value.name,
      category: form.value.category,
      lng: form.value.lng,
      lat: form.value.lat,
      province: form.value.province || null,
      city: form.value.city || null,
      district: form.value.district || null,
      address: form.value.address || null,
      phone: form.value.phone || null,
      driveScore: form.value.driveScore,
      parkingScore: form.value.parkingScore,
      roadScore: form.value.roadScore,
      rvFriendly: form.value.rvFriendly,
      signalQuality: form.value.signalQuality,
      petFriendly: form.value.petFriendly,
      campingAllowed: form.value.campingAllowed,
    }

    if (isNew.value) {
      await api.post('/admin/pois', payload)
      ElMessage.success('创建成功')
    } else {
      await api.put(`/admin/pois/${route.params.id}`, payload)
      ElMessage.success('保存成功')
    }
    router.push('/pois')
  } catch (e) {
    ElMessage.error(isNew.value ? '创建失败' : '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (!isNew.value && route.params.id) {
    fetchDetail(route.params.id)
  }
})
</script>
