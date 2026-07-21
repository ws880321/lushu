<template>
  <div class="sv">
    <div class="sv-card" v-if="route">
      <!-- Header -->
      <div class="sv-hero">
        <div class="sv-brand">🗺️ 路书</div>
        <h1>{{ route.title }}</h1>
        <div class="sv-stats">
          <span>{{ route.totalDays }} 天</span>
          <span>{{ route.totalDistanceKm || 0 }} km</span>
          <span>{{ route.startPoint }} → {{ route.endPoint }}</span>
        </div>
      </div>
      <!-- Waypoints by day -->
      <div class="sv-days" v-for="(wps, day) in route.itinerary" :key="day">
        <div class="sv-day-header">
          <span>Day {{ day }}</span>
          <small>{{ wps.length }} 个站点</small>
        </div>
        <div class="sv-timeline">
          <div v-for="(wp, i) in wps" :key="i" class="sv-wp">
            <div class="sv-dot" :class="wp.type"></div>
            <div class="sv-wp-body">
              <div class="sv-wp-name">{{ wp.name }}</div>
              <div class="sv-wp-type">{{ typeLabel(wp.type) }}</div>
              <div v-if="wp.tips" class="sv-wp-tips">{{ wp.tips }}</div>
            </div>
          </div>
        </div>
      </div>
      <!-- Footer -->
      <div class="sv-footer">
        <p>由 <b>路书</b> 生成</p>
        <p class="sv-cta">扫码或打开 <a href="https://www.wychen.net/app/">www.wychen.net/app/</a> 开始你的自驾</p>
        <van-button round block type="primary" @click="goApp" class="sv-btn">我也要生成路书</van-button>
      </div>
    </div>
    <van-loading v-else class="sv-loading" />
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'; import { useRoute, useRouter } from 'vue-router'
import api from '../utils/api'
const routeParam = useRoute(); const router = useRouter()
const route = ref(null)
const TYPES = { scenic: '景点', restaurant: '餐厅', hotel: '住宿', gas: '加油', rest: '休息', start: '出发', end: '终点', break: '休息点', food:'美食', photo:'拍照', custom:'自定义', service:'服务区' }
function typeLabel(t) { return TYPES[t] || t || '途经点' }
function goApp() { router.push('/generate') }
onMounted(async () => {
  try {
    const r = await api.get('/share/routes/' + routeParam.params.id)
    route.value = r.data.data
  } catch (e) {
    route.value = null
  }
})
</script>
<style scoped>
.sv { min-height: 100vh; background: #f5f5f5; padding-bottom: 40px; }
.sv-loading { display: flex; justify-content: center; padding-top: 100px; }
.sv-card { max-width: 480px; margin: 0 auto; }
.sv-hero { background: linear-gradient(135deg, #2E7D32 0%, #1B5E20 50%, #0D3B0F 100%); color: #fff; padding: 32px 20px 24px; text-align: center; }
.sv-brand { font-size: 13px; opacity: 0.8; margin-bottom: 8px; }
.sv-hero h1 { margin: 0 0 12px; font-size: 24px; }
.sv-stats { display: flex; justify-content: center; gap: 16px; font-size: 13px; opacity: 0.9; flex-wrap: wrap; }
.sv-days { margin: 8px 12px; background: #fff; border-radius: 12px; overflow: hidden; }
.sv-day-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; background: #f9f9f9; font-weight: 600; font-size: 15px; }
.sv-day-header small { color: #999; font-weight: 400; }
.sv-timeline { padding: 8px 0; }
.sv-wp { display: flex; gap: 12px; padding: 8px 16px; }
.sv-dot { width: 10px; height: 10px; border-radius: 50%; margin-top: 6px; flex-shrink: 0; background: #2E7D32; }
.sv-dot.scenic { background: #4CAF50; } .sv-dot.restaurant, .sv-dot.food { background: #FF9800; }
.sv-dot.hotel { background: #9C27B0; } .sv-dot.gas, .sv-dot.service { background: #E53935; }
.sv-dot.start { background: #1976D2; border-radius: 3px; } .sv-dot.end { background: #333; border: 2px solid #333; background: #fff; }
.sv-wp-name { font-size: 15px; font-weight: 600; color: #333; }
.sv-wp-type { font-size: 11px; color: #2E7D32; background: #e8f5e9; display: inline-block; padding: 1px 6px; border-radius: 4px; margin-top: 2px; }
.sv-wp-tips { font-size: 12px; color: #888; margin-top: 4px; line-height: 1.5; }
.sv-footer { text-align: center; padding: 24px 16px; }
.sv-footer p { color: #999; font-size: 13px; margin: 4px 0; }
.sv-cta { font-size: 13px; color: #666; margin: 12px 0; }
.sv-cta a { color: #2E7D32; }
.sv-btn { margin: 16px auto 0; max-width: 280px; background: #2E7D32; border-color: #2E7D32; }
</style>
