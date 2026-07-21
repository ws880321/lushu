<template>
  <div class="map-picker">
    <div ref="mapContainer" class="map-container"></div>
    <div v-if="coords" class="coord-tag">
      <van-tag closable @close="clearCoords" type="primary">
        {{ coords.lng.toFixed(6) }}, {{ coords.lat.toFixed(6) }}
      </van-tag>
    </div>
    <p v-else class="hint">点击地图选择坐标</p>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: null,
  },
  amapKey: {
    type: String,
    default: import.meta.env.VITE_AMAP_JS_KEY || '2d139f2097244f71e55462cb0d9b1f1a',
  },
})

const emit = defineEmits(['update:modelValue'])

const mapContainer = ref(null)
const coords = ref(props.modelValue)
let map = null
let marker = null

function clearCoords() {
  coords.value = null
  emit('update:modelValue', null)
  if (marker) {
    map.remove(marker)
    marker = null
  }
}

function loadAmapScript() {
  return new Promise((resolve, reject) => {
    if (window.AMap) {
      resolve(window.AMap)
      return
    }
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${props.amapKey}`
    script.async = true
    script.onload = () => resolve(window.AMap)
    script.onerror = () => reject(new Error('Failed to load AMap JS API'))
    document.head.appendChild(script)
  })
}

onMounted(async () => {
  await nextTick()
  try {
    const AMap = await loadAmapScript()
    map = new AMap.Map(mapContainer.value, {
      zoom: 5,
      center: [104.0, 35.0],
    })
    map.on('click', (e) => {
      const { lng, lat } = e.lnglat
      coords.value = { lng, lat }
      emit('update:modelValue', { lng, lat })
      if (marker) {
        marker.setPosition([lng, lat])
      } else {
        marker = new AMap.Marker({ position: [lng, lat], map })
      }
    })
    if (coords.value) {
      const { lng, lat } = coords.value
      map.setCenter([lng, lat])
      map.setZoom(14)
      marker = new AMap.Marker({ position: [lng, lat], map })
    }
  } catch (e) {
    console.error('MapPicker init error:', e)
  }
})
</script>

<style scoped>
.map-container {
  width: 100%;
  height: 280px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
}
.coord-tag {
  margin-top: 8px;
  text-align: center;
}
.hint {
  margin-top: 8px;
  font-size: 13px;
  color: #999;
  text-align: center;
}
</style>
