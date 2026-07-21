<template>
  <div class="db">
    <div class="db-map-wrap"><MapView height="220px" :center="center" :zoom="13" :markers="posMarker" /></div>
    <div class="db-status">
      <div class="fuel-card">
        <div class="fuel-label">📍 当前位置</div>
        <div class="fuel-addr">{{curAddr||'定位中...'}}</div>
        <van-divider />
        <div class="fuel-row"><span>车辆</span><span class="fuel-veh" @click="$router.push('/profile')">{{vehicleName||'未设置'}} <van-icon name="arrow" /></span></div>
        <div class="fuel-label">续航里程</div>
        <div class="fuel-val">{{fuelLeft}}/<small>{{fuelMax}}</small> km</div>
        <van-progress :percentage="fuelPercent" :color="fuelPercent>30?'#2E7D32':'#E53935'" stroke-width="10" />
        <div class="fuel-status" :class="fuelPercent>30?'ok':'warn'">{{fuelPercent>30?'够用':'需加油'}}</div>
      </div>
    </div>
    <div class="alerts">
      <h4>周边补给</h4>
      <van-empty v-if="!alerts.length" description="暂无周边信息" />
      <AlertItem v-for="(a,i) in alerts" :key="i" :item="a" @click="onAlertClick(a)" />
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted, onUnmounted } from 'vue'; import api from '../utils/api'; import MapView from '../components/MapView.vue'; import AlertItem from '../components/AlertItem.vue'
import { reverseGeocode } from '../utils/amap'
const center=ref([104,30]), posMarker=ref([]), alerts=ref([]), loading=ref(true), curAddr=ref('')
const fuelLeft=ref(420), fuelMax=ref(600), fuelPercent=ref(70), vehicleName=ref('')
let timer = null

async function reverseGeo(lng, lat) {
  try { curAddr.value = await reverseGeocode(lng, lat) } catch { curAddr.value = '' }
}
onMounted(async()=>{
  // Fetch vehicle data + active route distance to calculate real fuel status
  let rangeFull=420,routeDistance=0
  try{
    const r=await api.get('/vehicles');const vs=r.data.data
    if(vs&&vs.length){const v=vs[0];rangeFull=v.rangeFull||420;fuelMax.value=rangeFull;vehicleName.value=v.name||''}
  }catch{}
  try{
    const r=await api.get('/routes',{params:{size:1}});const routes=r.data.data?.content||r.data.data||[]
    if(routes.length){routeDistance=parseFloat(routes[0].totalDistance||0)||0}
  }catch{}
  fuelLeft.value=Math.max(0,rangeFull-routeDistance)
  fuelPercent.value=rangeFull>0?Math.round((fuelLeft.value/rangeFull)*100):100
  if(navigator.geolocation){navigator.geolocation.getCurrentPosition(p=>{const lng=p.coords.longitude,lat=p.coords.latitude;center.value=[lng,lat];posMarker.value=[{id:0,position:[lng,lat]}];reverseGeo(lng,lat);fetchAlerts(lng,lat);loading.value=false},()=>{loading.value=false;curAddr.value='无法获取位置'})}else{loading.value=false;curAddr.value='不支持'}
  timer=setInterval(refreshPosition,30000)
})
onUnmounted(()=>{if(timer)clearInterval(timer)})
function refreshPosition(){
  if(!navigator.geolocation) return
  navigator.geolocation.getCurrentPosition(p=>{center.value=[p.coords.longitude,p.coords.latitude];posMarker.value=[{id:0,position:[p.coords.longitude,p.coords.latitude]}];reverseGeo(p.coords.longitude,p.coords.latitude);fetchAlerts(p.coords.longitude,p.coords.latitude)})
}
async function fetchAlerts(lng,lat){
  try{const r=await api.get('/nearby/alerts',{params:{lng,lat,radius:50000,vehicleRangeLeft:fuelLeft.value}});alerts.value=r.data.data?.alerts||[]}catch{}
}
function onAlertClick(a){window.open(`https://uri.amap.com/navigation?to=${a.poiLng},${a.poiLat},${a.poiName}&mode=car&callnative=1`,'_blank')}
</script>
<style scoped>
.db{padding-bottom:20px}.db-map-wrap{margin-bottom:12px}
.db-status{padding:0 12px}.fuel-card{background:#fff;border-radius:10px;padding:16px;box-shadow:0 1px 4px rgba(0,0,0,.06)}
.fuel-row{display:flex;justify-content:space-between;align-items:center;padding:4px 0;font-size:13px}.fuel-veh{color:#2E7D32;cursor:pointer}.fuel-label{font-size:13px;color:#999}.fuel-addr{font-size:15px;color:#333;margin:4px 0;word-break:break-all}.fuel-val{font-size:28px;font-weight:700;color:#333;margin:4px 0 8px}.fuel-val small{font-size:16px;color:#999;font-weight:400}
.fuel-status{font-size:13px;margin-top:6px}.fuel-status.ok{color:#2E7D32}.fuel-status.warn{color:#E53935}
.alerts{padding:12px}.alerts h4{font-size:16px;margin:0 0 10px}
</style>
