<template>
  <div class="rec">
    <div class="hd"><h3>记录行程</h3></div>

    <div class="photo-section" v-if="photos.length">
      <div v-for="(p,i) in photos" :key="i" class="photo-item" @click="previewPhoto(i)">
        <img :src="p" />
        <van-icon name="cross" class="photo-del" @click.stop="photos.splice(i,1)" />
      </div>
    </div>

    <div class="card">
      <van-button block @click="getLocation" :loading="locating" class="loc-btn">
        <template v-if="locating">📍 定位中...</template>
        <template v-else-if="locName">📍 {{ locName.substring(0,24) }}</template>
        <template v-else-if="isWx">📍 获取当前位置（微信内）</template>
        <template v-else>📍 获取当前位置</template>
      </van-button>

      <van-button block @click="showMapPicker=true" class="map-btn">
        🗺️ 地图选点
      </van-button>

      <van-field v-model="name" placeholder="地点名称" label="名称">
        <template #left-icon><van-icon name="label-o" /></template>
      </van-field>

      <div class="row" @click="showType=true"><span>类型</span>
        <van-tag :type="typeColor[typeIdx]" size="medium">{{types[typeIdx]}}</van-tag>
        <van-icon name="arrow" color="#ccc" />
      </div>

      <div class="stepper-row"><span>第{{dayNum}}天</span><van-stepper v-model="dayNum" :min="1" :max="30" /></div>

      <van-field v-model="note" placeholder="备注（选填）" label="备注" />

      <div class="action-row">
        <van-uploader v-model="fileList" :max-count="1" :after-read="onPhotoRead" accept="image/*" capture="camera">
          <van-button icon="photo-o" size="small" plain type="default">拍照/相册</van-button>
        </van-uploader>
      </div>

      <van-button round block type="primary" :loading="saving" @click="savePoint" class="save-btn">
        {{ saving ? '记录中...' : '✓ 记录此点' }}
      </van-button>
    </div>

    <div v-if="records.length" class="records">
      <div class="rec-header">
        <h4>已记录 {{records.length}} 个点</h4>
        <span class="undo" @click="undoLast" v-if="records.length">撤销上一个</span>
      </div>
      <div v-for="(r,i) in records" :key="i" class="rec-item">
        <img v-if="r.photo" :src="r.photo" class="rec-thumb" />
        <div class="rec-info">
          <div class="rec-name">{{r.name}}</div>
          <div class="rec-meta"><span class="rec-type">{{typeLabel(r.type)}}</span> D{{r.dayNumber}}<span v-if="r.note"> · {{r.note}}</span></div>
        </div>
      </div>
    </div>

    <van-button v-if="routeId" round block type="success" @click="finishTrip" class="finish-btn">
      ✓ 完成行程记录
    </van-button>

    <van-action-sheet v-model:show="showType" :actions="typeActions" @select="onTypeSelect" />
    <van-image-preview v-model:show="showPreview" :images="photos" :start-position="previewIdx" />

    <van-popup v-model:show="showMapPicker" position="bottom" :style="{ height: '60vh' }" round>
      <div class="map-picker-header">
        <span>点击地图选择位置</span>
        <van-icon name="cross" @click="showMapPicker=false" />
      </div>
      <MapPicker v-model="mapPickerCoords" />
      <div class="map-picker-footer">
        <van-button block type="primary" @click="onMapPickConfirm" :disabled="!mapPickerCoords">确定选点</van-button>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'
import api from '../utils/api'
import { isWeChat, initWxJsSdk, wxGetLocation } from '../utils/wechat'
import MapPicker from '../components/MapPicker.vue'

const routerParam = useRoute()

const name = ref(''), typeIdx = ref(0), dayNum = ref(1), note = ref('')
const lng = ref(null), lat = ref(null), saving = ref(false)
const locName = ref(''), locating = ref(false), isWx = ref(false)
const records = ref([]), routeId = ref(null), showType = ref(false)
const photos = ref([]), fileList = ref([]), showPreview = ref(false), previewIdx = ref(0)
const photoFile = ref(null)
const showMapPicker = ref(false), mapPickerCoords = ref(null)

const types = ['景点', '美食', '住宿', '加油', '拍照', '其他']
const typeCodes = ['scenic', 'food', 'hotel', 'gas', 'photo', 'custom']
const typeColor = ['success','warning','','danger','','info']
const typeActions = types.map((t, i) => ({ name: t, value: i }))
const TYPE_LABELS={scenic:'景点',food:'美食',hotel:'住宿',gas:'加油',photo:'拍照',custom:'其他'}
function typeLabel(t){return TYPE_LABELS[t]||t}

const POI_CATEGORIES = [
  ['公园','景区','古镇','寺庙','博物馆','山','湖','河','峡谷','草原','森林','海滩','长城','塔','桥','园林','故居'],
  ['餐厅','美食','火锅','面','小吃','烧烤','酒楼','饭店','馆'],
  ['酒店','宾馆','民宿','客栈','青旅','度假村'],
  ['加油站','充电站','汽修'],
]

function onTypeSelect(a) { typeIdx.value = a.value; showType.value = false }

function autoDetectType(addr) {
  if (!addr) return
  const lower = addr.toLowerCase()
  for (let i = 0; i < POI_CATEGORIES.length; i++) {
    for (const kw of POI_CATEGORIES[i]) {
      if (addr.includes(kw)) { typeIdx.value = i; return }
    }
  }
}

onMounted(async () => {
  isWx.value = isWeChat()
  let targetRouteId = null
  const urlId = routerParam.query.id
  if (urlId) targetRouteId = String(urlId)

  // Proactively request location permission
  if (!isWx.value && navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (pos) => { lng.value = pos.coords.longitude; lat.value = pos.coords.latitude; reverseGeoOnce() },
      () => {} // user denied — will prompt manually later
    )
  }

  try {
    if (!targetRouteId) {
      const r = await api.get('/routes', { params: { size: 5, sortBy: 'createdAt', sortDir: 'desc' } })
      const routes = r.data.data?.content || []
      const active = routes.find((rt) => rt.status === 2)
      if (active) targetRouteId = String(active.id)
    }
    if (targetRouteId) {
      routeId.value = targetRouteId
      const detailR = await api.get('/routes/' + targetRouteId)
      const detail = detailR.data.data
      if (detail?.itinerary) {
        const loaded = []
        for (const day of detail.itinerary) {
          for (const wp of day.waypoints || []) {
            loaded.push({
              name: wp.name, type: wp.type, dayNumber: day.day || 1,
              note: wp.description || '', photo: wp.photoUrl || null
            })
          }
        }
        records.value = loaded
        if (loaded.length > 0) dayNum.value = loaded[loaded.length - 1].dayNumber || 1
      }
    }
  } catch {}
})

async function getLocation() {
  locating.value = true; locName.value = ''
  if (isWx.value) {
    try {
      const ready = await initWxJsSdk()
      if (ready) {
        const pos = await wxGetLocation()
        if (pos) { lng.value = pos.lng; lat.value = pos.lat; await reverseGeoOnce(); locating.value = false; return }
      }
    } catch {}
  }
  if (!navigator.geolocation) {
    locating.value = false
    if (isWx.value) return showToast('微信内定位失败，请手动输入位置名称')
    return showToast('浏览器不支持定位')
  }
  try {
    const pos = await new Promise((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, { timeout: 15000, maximumAge: 60000, enableHighAccuracy: false })
    })
    lng.value = pos.coords.longitude; lat.value = pos.coords.latitude
  } catch (e) {
    locating.value = false
    if (isWx.value) return showToast('微信内定位受限，请手动输入位置')
    if (e.code === 1) return showToast('请允许浏览器获取位置权限')
    if (e.code === 2) return showToast('获取位置超时')
    if (e.code === 3) return showToast('位置不可用')
    return showToast('定位失败')
  }
  await reverseGeoOnce()
  locating.value = false
}

import { reverseGeocode } from '../utils/amap'

async function reverseGeoOnce() {
  try {
    const addr = await reverseGeocode(lng.value, lat.value)
    locName.value = addr
    if (!name.value && !addr.match(/^\d+\.\d+/)) { name.value = addr; autoDetectType(addr) }
  } catch {
    locName.value = lng.value.toFixed(4) + ',' + lat.value.toFixed(4)
  }
}

function onPhotoRead(item) {
  // Vant default result-type="dataUrl": item.content = "data:image/jpeg;base64,..."
  // item.file is the native File (available in modern browsers)
  const src = item.content || item.objectUrl || item.url
  if (src) photos.value = [src]
  if (item.file instanceof File) {
    photoFile.value = item.file
  } else if (item.content && typeof item.content === 'string' && item.content.startsWith('data:')) {
    // Fallback: construct File from base64 data URL
    try {
      const [hdr, data] = item.content.split(',')
      const mime = (hdr.split(';')[0].split(':')[1]) || 'image/jpeg'
      const raw = window.atob(data)
      const u8 = new Uint8Array(raw.length)
      for (let i = 0; i < raw.length; i++) u8[i] = raw.charCodeAt(i)
      photoFile.value = new File([u8], 'photo_' + Date.now() + '.jpg', { type: mime })
    } catch { /* ignore */ }
  }
  fileList.value = [{ url: photos.value[0] }]
}

function previewPhoto(i) { previewIdx.value = i; showPreview.value = true }

async function savePoint() {
  if (!name.value) return showToast('请输入地点名称')
  if (lng.value == null || lat.value == null) return showToast('请先获取位置')
  saving.value = true

  let photoUrl = null
  if (photoFile.value) {
    try {
      const form = new FormData()
      form.append('file', photoFile.value)
      const resp = await api.post('/upload', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      if (resp.data.code === 0 && resp.data.data?.url) {
        photoUrl = resp.data.data.url
      } else {
        throw new Error(resp.data.message || '上传失败')
      }
    } catch (e) {
      console.error(e)
      saving.value = false
      showToast('照片上传失败: ' + (e.response?.data?.message || e.message || ''))
      return
    }
  }

  try {
    const payload = {
      name: name.value, type: typeCodes[typeIdx.value],
      dayNumber: dayNum.value, note: note.value,
      lng: lng.value, lat: lat.value,
      photoUrl: photoUrl,
      ...(routeId.value ? { routeId: routeId.value } : {}),
    }
    const r = await api.post('/routes/record', payload)
    const photo = photos.value.length > 0 ? photos.value[photos.value.length - 1] : null
    records.value.push({
      name: name.value, type: typeCodes[typeIdx.value],
      dayNumber: dayNum.value, note: note.value, photo,
    })
    routeId.value = r.data.data.routeId
    const locs = records.value.map(r => r.name)
    if (locs.length >= 2) {
      await api.put('/routes/' + routeId.value, { title: locs[0] + ' → ' + locs[locs.length - 1] + '行程' })
    }
    name.value = ''; note.value = ''; photos.value = []; fileList.value = []; photoFile.value = null
    showToast('已记录')
  } catch (e) {
    console.error(e)
    showToast('记录失败')
  } finally {
    saving.value = false
  }
}

async function finishTrip() {
  try { await api.post('/routes/' + routeId.value + '/finish'); showToast('行程完成！'); setTimeout(() => window.history.back(), 1500) }
  catch { showToast('操作失败') }
}

function undoLast() { records.value.pop(); showToast('已撤销') }

function onMapPickConfirm() {
  if (!mapPickerCoords.value) return
  lng.value = mapPickerCoords.value.lng
  lat.value = mapPickerCoords.value.lat
  showMapPicker.value = false
  reverseGeoOnce()
}
</script>

<style scoped>
.rec { padding: 12px; padding-bottom: 80px; }
.hd { padding: 8px; }
.hd h3 { margin: 0; font-size: 18px; }

.photo-section { display: flex; gap: 8px; padding: 8px 0; overflow-x: auto; }
.photo-item { position: relative; width: 80px; height: 80px; border-radius: 8px; overflow: hidden; flex-shrink: 0; }
.photo-item img { width: 100%; height: 100%; object-fit: cover; }
.photo-del { position: absolute; top: 2px; right: 2px; background: rgba(0,0,0,.5); color: #fff; border-radius: 50%; padding: 2px; font-size: 12px; }

.card { background: #fff; border-radius: 12px; padding: 14px; box-shadow: 0 1px 6px rgba(0,0,0,.06); margin-bottom: 12px; }
.row { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #f0f0f0; cursor: pointer; }
.stepper-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; }

.loc-btn { margin-bottom: 10px; background: #e8f5e9; border: 1px dashed #2E7D32; color: #2E7D32; font-size: 14px; border-radius: 8px; }

.map-btn { margin-bottom: 10px; background: #e3f2fd; border: 1px dashed #1565C0; color: #1565C0; font-size: 14px; border-radius: 8px; }

.map-picker-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; font-size: 15px; font-weight: 600; }
.map-picker-footer { padding: 12px 16px; }

.action-row { display: flex; gap: 8px; padding: 8px 0; }
.save-btn { margin-top: 12px; background: #2E7D32; border-color: #2E7D32; }

.records { margin-top: 16px; }
.rec-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.rec-header h4 { font-size: 15px; margin: 0; }
.undo { font-size: 12px; color: #E53935; cursor: pointer; }

.rec-item { padding: 10px 12px; background: #fff; border-radius: 10px; margin-bottom: 8px; display: flex; gap: 10px; align-items: center; }
.rec-thumb { width: 48px; height: 48px; border-radius: 6px; object-fit: cover; flex-shrink: 0; }
.rec-info { flex: 1; }
.rec-name { font-size: 14px; font-weight: 600; color: #333; }
.rec-meta { font-size: 12px; color: #999; margin-top: 2px; display: flex; gap: 6px; align-items: center; }
.rec-type { font-size: 10px; padding: 1px 6px; border-radius: 3px; background: #e8f5e9; color: #2E7D32; }

.finish-btn { margin-top: 16px; }
</style>
