<template>
  <!-- Skeleton loading -->
  <div class="rd-skeleton" v-if="!route">
    <div class="sk-map"></div>
    <div class="sk-cost-bar">
      <div class="sk-cost-item" v-for="i in 3" :key="i"><div class="sk-line sk-w40"></div><div class="sk-line sk-w60 sk-short"></div></div>
    </div>
    <div class="sk-days">
      <div class="sk-pill" v-for="i in 4" :key="i"></div>
    </div>
    <div class="sk-timeline">
      <div class="sk-wp" v-for="i in 3" :key="i">
        <div class="sk-dot-col"><div class="sk-dot"></div><div class="sk-vline"></div></div>
        <div class="sk-wp-card">
          <div class="sk-line sk-w70"></div>
          <div class="sk-line sk-w30 sk-short"></div>
          <div class="sk-line sk-w50 sk-short"></div>
        </div>
      </div>
    </div>
  </div>

  <!-- Real content -->
  <div class="rd" v-else>
    <van-nav-bar title="路线详情" left-text="返回" left-arrow @click-left="$router.back()">
      <template #right>
        <van-button v-if="route?.status===2" size="mini" plain type="warning" @click="$router.push('/record?id='+id)" style="margin-right:6px">继续记录</van-button>
        <van-button v-if="route?.status===1" size="mini" plain type="primary" @click="$router.push('/record?id='+id)" style="margin-right:6px">添加途经点</van-button>
        <van-button v-if="route?.status!==2" size="mini" plain type="success" @click="publishRoute" style="margin-right:6px">发布</van-button>
        <van-icon name="edit" size="18" color="#666" style="margin-right:8px" @click="startEdit" />
        <van-icon name="share-o" size="18" color="#666" style="margin-right:8px" @click="shareRoute" />
        <van-icon name="photograph" size="18" color="#FF9800" style="margin-right:8px" @click="generatePoster" />
        <van-icon name="delete-o" size="18" color="#E53935" style="margin-right:8px" @click="confirmDelete" />
        <van-icon name="star-o" size="18" :color="favorited?'#FF9800':'#999'" @click="toggleFav" />
      </template>
    </van-nav-bar>
    <div class="rd-map-wrap">
      <MapView ref="mapRef" height="280px" :center="center" :zoom="10" :markers="markers" :polylines="polylines" @markerClick="onMarkerClick" />
      <div class="rd-overlay">
        <h3>{{route.title}}</h3>
        <p>{{route.totalDistanceKm||0}}km / {{route.totalDays}}天 / ¥{{route.estimatedCost?.totalYuan||0}}</p>
      </div>
    </div>
    <div class="rd-cost-bar">
      <div class="cost-item"><span class="cost-num">¥{{route.estimatedCost?.tollYuan||0}}</span><span class="cost-label">过路费</span></div>
      <div class="cost-item"><span class="cost-num">¥{{route.estimatedCost?.fuelYuan||0}}</span><span class="cost-label">油费</span></div>
      <div class="cost-item"><span class="cost-num">¥{{route.estimatedCost?.totalYuan||0}}</span><span class="cost-label">总花费</span></div>
    </div>
    <div class="rd-weather" v-if="weather&&weather.forecasts">
      <div class="w-title">📅 天气预报</div>
      <div class="w-fc" v-if="weather.forecasts">
        <div v-for="f in weather.forecasts" :key="f.date" class="w-day">
          <div class="w-date">{{f.date.substring(5)}}</div>
          <div class="w-dw">{{f.dayWeather||'?'}}</div>
          <div class="w-nt">{{f.dayTemp||0}}°/{{f.nightTemp||0}}°</div>
        </div>
      </div>
    </div>
    <div class="rd-days">
      <div v-for="d in route.totalDays" :key="d" class="day-tab" :class="{active:activeDay===d}" @click="selectDay(d)">第{{d}}天</div>
    </div>
    <div class="rd-timeline" v-if="currentDay">
      <div class="day-header">Day {{activeDay}} / {{currentDay.distanceKm||0}}km / {{currentDay.driveTimeMin||0}}分钟</div>
      <div class="day-summary" v-if="currentDay.summary">{{currentDay.summary}}</div>
      <WaypointCard v-for="(w,i) in currentDay.waypoints" :key="i" :name="w.name" :type="w.type" :arrival="w.arrival" :departure="w.departure" :tips="w.description||w.tips" :distanceFromPrevKm="w.distanceFromPrevKm" :stayMin="w.stayMin" :isLast="i===currentDay.waypoints.length-1" @click="onWaypointClick(w)" />
    </div>
    
    <van-dialog v-model:show="showDialog" :title="selectedWp?.name" show-cancel-button>
      <div class="dlg-body" v-if="selectedWp">
        <div class="dlg-type"><van-tag :type="typeTag(selectedWp.type)" size="medium">{{typeLabel(selectedWp.type)}}</van-tag></div>
        <p class="dlg-info">{{selectedWp.description||selectedWp.tips||'暂无详细信息'}}</p>
        <div class="dlg-scores" v-if="selectedWp.driveScore||selectedWp.parkingScore||selectedWp.roadScore">
          <span v-if="selectedWp.driveScore" class="score-badge drive">🚗 驾驶 {{selectedWp.driveScore}}</span>
          <span v-if="selectedWp.parkingScore" class="score-badge park">🅿️ 停车 {{selectedWp.parkingScore}}</span>
          <span v-if="selectedWp.roadScore" class="score-badge road">🛣️ 路况 {{selectedWp.roadScore}}</span>
        </div>
        <div class="dlg-stats" v-if="selectedWp.stayMin || selectedWp.distanceFromPrevKm">
          <span v-if="selectedWp.stayMin">🕐 建议停留 {{selectedWp.stayMin}} 分钟</span>
          <span v-if="selectedWp.distanceFromPrevKm">📏 距上站 {{selectedWp.distanceFromPrevKm}}km</span>
        </div>
        <div class="dlg-arrival" v-if="selectedWp.arrival">到达: {{selectedWp.arrival}} <span v-if="selectedWp.departure">→ 离开: {{selectedWp.departure}}</span></div>
      </div>
      <template #footer>
        <van-button size="small" @click="navigateTo(selectedWp)" type="primary">🧭 导航前往</van-button>
      </template>
    </van-dialog>

    <van-dialog v-model:show="showEdit" title="编辑路线" @confirm="saveEdit" show-cancel-button>
      <van-field v-model="editTitle" label="标题" placeholder="路线标题" />
    </van-dialog>

    <van-dialog v-model:show="showDel" title="确认删除" message="删除后无法恢复，确定删除这条路线吗？" @confirm="doDelete" show-cancel-button />

    <!-- AI Chat Bar -->
    <div class="ai-chat-bar" v-if="route">
      <van-field v-model="chatMsg" placeholder="AI 调整路线，如：第三天加一个景点" @keyup.enter="sendAdjust" :disabled="adjusting">
        <template #button>
          <van-button size="small" type="primary" :loading="adjusting" @click="sendAdjust">调整</van-button>
        </template>
      </van-field>
      <div class="chat-hints">
        <van-tag size="medium" plain @click="quickAdjust('改成5天')">改成5天</van-tag>
        <van-tag size="medium" plain @click="quickAdjust('第三天多玩一个景点')">加景点</van-tag>
        <van-tag size="medium" plain @click="quickAdjust('加入露营地点')">加露营</van-tag>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'; import { useRoute, useRouter } from 'vue-router'; import { showToast } from 'vant'
import api from '../utils/api'; import { getCachedRoute, cacheRoute } from '../utils/cache'; import { buildPolyline, buildMarkers } from '../utils/map'
import { exportRoutePDF } from "../utils/pdf"; import { generateRoutePoster } from "../utils/poster"; import MapView from '../components/MapView.vue'; import WaypointCard from '../components/WaypointCard.vue'
const routeParam = useRoute(); const router = useRouter(); const id = routeParam.params.id
const route=ref(null), activeDay=ref(1), currentDay=ref(null)
const markers=ref([]), polylines=ref([]), center=ref([104,30])
const showDialog=ref(false), selectedWp=ref(null), mapRef=ref(null), favorited=ref(false), weather=ref(null)
const chatMsg=ref(''), adjusting=ref(false)

onMounted(async()=>{await loadRoute();selectDay(1);checkFav();fetchWeather()})
async function checkFav(){try{const r=await api.get('/favorites',{params:{type:'route'}});favorited.value=(r.data.data||[]).some(f=>f.targetId==id)}catch{}}
async function fetchWeather(){try{const r=await api.get('/share/weather',{params:{city:route.value?.endPoint||'成都'}});weather.value=r.data.data}catch{}}
async function publishRoute(){try{await api.post('/routes/'+id+'/publish');showToast('已发布到社区')}catch{showToast('发布失败')}}
async function loadRoute(){
  try{
    const cached=getCachedRoute(id)
    if(!cached||Date.now()-parseInt(localStorage.getItem('route_'+id+'_ts')||0)>86400000){
      const r=await api.get('/routes/'+id); route.value=r.data.data; cacheRoute(id,r.data.data)
    }else{route.value=cached}
  }catch(e){const cached=getCachedRoute(id);if(cached){route.value=cached;showToast('当前离线，显示缓存')}else{showToast('加载失败')}}
}
function selectDay(day){
  activeDay.value=day
  if(!route.value?.itinerary) return
  currentDay.value=route.value.itinerary.find(d=>d.day===day)
  if(currentDay.value?.waypoints){
    markers.value=buildMarkers(currentDay.value.waypoints,day)
    polylines.value=buildPolyline(currentDay.value.waypoints,day)
    if(currentDay.value.waypoints[0]) center.value=[currentDay.value.waypoints[0].lng,currentDay.value.waypoints[0].lat]
    nextTick(()=>{const map=mapRef.value?.getMap();if(map&&currentDay.value.waypoints.length>1){map.setFitView(currentDay.value.waypoints.map(w=>[w.lng,w.lat]))}})
  }
}
const TYPES={scenic:'景点',restaurant:'餐厅',hotel:'住宿',gas:'加油',rest:'休息',start:'出发',end:'终点',break:'休息点'}
const TYPE_TAGS={scenic:'success',restaurant:'warning',hotel:'',gas:'danger',rest:'',start:'primary',end:'danger',break:'warning'}
function typeLabel(t){return TYPES[t]||t||'途经点'}
function typeTag(t){return TYPE_TAGS[t]||'info'}

function onMarkerClick(m){selectedWp.value=m;showDialog.value=true}
function onWaypointClick(w){selectedWp.value=w;showDialog.value=true}
function navigateTo(wp){window.open(`https://uri.amap.com/navigation?to=${wp.lng},${wp.lat},${wp.name}&mode=car&callnative=1`,'_blank')}
const shareLink = computed(() => `${window.location.origin}/share/${id}`)
function shareRoute(){
  if (navigator.share) {
    navigator.share({ title: route.value?.title || '路书', text: `自驾路书: ${route.value?.title}`, url: shareLink.value }).catch(() => {})
  } else {
    navigator.clipboard.writeText(shareLink.value).then(() => showToast('链接已复制，可粘贴到微信分享'))
  }
}
const showEdit=ref(false),showDel=ref(false),editTitle=ref('')
function startEdit(){
  // If route is still recording, redirect to record page with id
  if(route.value?.status===2){router.push('/record?id='+id);return}
  editTitle.value=route.value?.title||'';showEdit.value=true
}
async function saveEdit(){
  try{await api.put('/routes/'+id,{title:editTitle.value});route.value.title=editTitle.value;showToast('已更新')}catch{showToast('更新失败')}
}
function confirmDelete(){showDel.value=true}
async function doDelete(){
  try{await api.delete('/routes/'+id);showToast('已删除');router.replace('/home')}catch{showToast('删除失败')}
}
async function toggleFav(){
  try{
    if(favorited.value){await api.delete('/favorites',{params:{type:'route',targetId:id}});favorited.value=false;showToast('已取消收藏')}
    else{await api.post('/favorites',{favType:'route',targetId:parseInt(id)});favorited.value=true;showToast('已收藏')}
  }catch{showToast('操作失败')}
}
async function generatePoster(){
  try{
    showToast('正在生成海报...')
    const dataUrl = await generateRoutePoster(route.value)
    if(dataUrl){
      const a = document.createElement('a'); a.href = dataUrl; a.download = `${route.value?.title||'路书'}_海报.jpg`
      a.click(); showToast('海报已保存')
    }
  }catch{showToast('生成海报失败')}
}

async function quickAdjust(msg){ chatMsg.value=msg; sendAdjust() }
async function sendAdjust(){
  if(!chatMsg.value.trim()||adjusting.value) return
  adjusting.value=true
  try{
    const r=await api.post('/routes/'+id+'/adjust',{message:chatMsg.value.trim()})
    route.value=r.data.data; cacheRoute(id,r.data.data)
    chatMsg.value=''; selectDay(1); showToast('路线已调整')
  }catch{showToast('调整失败，请稍后重试')}
  finally{adjusting.value=false}
}
</script>
<style scoped>
.rd{padding-bottom:20px}.rd-cost-bar{display:flex;margin:8px 12px;gap:8px}.cost-item{flex:1;text-align:center;background:#fff;border-radius:10px;padding:10px 8px;box-shadow:0 1px 4px rgba(0,0,0,.06)}.cost-num{display:block;font-size:18px;font-weight:700;color:#2E7D32}.cost-label{font-size:11px;color:#999;margin-top:2px}
.rd-weather{margin:8px 12px;background:linear-gradient(135deg,#e3f2fd,#bbdefb);border-radius:10px;padding:12px}.w-title{font-size:13px;font-weight:600;color:#1565C0;margin-bottom:8px}
.w-fc{display:flex;gap:8px;margin-top:4px;overflow-x:auto}.w-day{flex-shrink:0;text-align:center;background:rgba(255,255,255,.6);border-radius:8px;padding:6px 10px}.w-date{font-size:11px;color:#999}.w-dw{font-size:13px;font-weight:600;color:#333}.w-nt{font-size:11px;color:#666}
.rd-loading{display:flex;justify-content:center;padding-top:100px}

/* Skeleton */
.rd-skeleton{padding-bottom:20px}
.sk-map{height:280px;background:linear-gradient(110deg,#ececec 30%,#f5f5f5 50%,#ececec 70%);background-size:200% 100%;animation:shimmer 1.5s ease-in-out infinite;border-radius:0}
.sk-cost-bar{display:flex;margin:8px 12px;gap:8px}
.sk-cost-item{flex:1;text-align:center;background:#fff;border-radius:10px;padding:12px 8px;display:flex;flex-direction:column;align-items:center;gap:6px}
.sk-days{display:flex;gap:8px;overflow-x:auto;padding:10px 8px}
.sk-pill{flex-shrink:0;width:72px;height:32px;border-radius:16px;background:linear-gradient(110deg,#ececec 30%,#f5f5f5 50%,#ececec 70%);background-size:200% 100%;animation:shimmer 1.5s ease-in-out infinite}
.sk-timeline{padding:0 12px;margin-top:8px}
.sk-wp{display:flex;gap:12px;margin-bottom:16px}
.sk-dot-col{display:flex;flex-direction:column;align-items:center;width:24px;flex-shrink:0}
.sk-dot{width:14px;height:14px;border-radius:50%;background:#ddd}
.sk-vline{width:2px;flex:1;min-height:40px;background:#e8e8e8;margin-top:4px}
.sk-wp-card{flex:1;background:#fff;border-radius:12px;padding:14px;display:flex;flex-direction:column;gap:8px}
.sk-line{height:14px;border-radius:6px;background:linear-gradient(110deg,#ececec 30%,#f5f5f5 50%,#ececec 70%);background-size:200% 100%;animation:shimmer 1.5s ease-in-out infinite}
.sk-w40{width:40%}.sk-w60{width:60%}.sk-w70{width:70%}.sk-w30{width:30%}.sk-w50{width:50%}
.sk-short{height:10px}

@keyframes shimmer{0%{background-position:200% 0}100%{background-position:-200% 0}}

/* AI Chat */
.ai-chat-bar{position:fixed;bottom:0;left:0;right:0;background:#fff;border-top:1px solid #eee;padding:8px 12px;z-index:100;box-shadow:0 -2px 8px rgba(0,0,0,.04)}
.chat-hints{display:flex;gap:6px;padding:6px 0 2px;overflow-x:auto}
.chat-hints .van-tag{cursor:pointer;flex-shrink:0}
.rd{padding-bottom:120px}
.rd-map-wrap{position:relative}.rd-overlay{position:absolute;top:8px;left:8px;right:8px;background:rgba(255,255,255,.92);border-radius:8px;padding:10px 14px}.rd-overlay h3{margin:0;font-size:16px}.rd-overlay p{margin:4px 0 0;font-size:12px;color:#666}
.rd-days{display:flex;gap:8px;overflow-x:auto;padding:10px 8px}.day-tab{flex-shrink:0;padding:6px 16px;border-radius:16px;background:#f0f0f0;font-size:13px;cursor:pointer}.day-tab.active{background:#2E7D32;color:#fff}
.day-header{padding:8px;font-size:14px;font-weight:600;color:#333}.day-summary{font-size:12px;color:#999;padding:0 8px 8px;font-style:italic}.rd-timeline{padding:0 12px}
.dlg-body{padding:8px 16px 16px}.dlg-type{margin-bottom:8px}.dlg-info{font-size:14px;color:#555;line-height:1.6}
.dlg-scores{display:flex;gap:10px;margin-top:10px;flex-wrap:wrap}
.score-badge{font-size:12px;padding:3px 10px;border-radius:12px;font-weight:500}.score-badge.drive{background:#e8f5e9;color:#2E7D32}.score-badge.park{background:#e3f2fd;color:#1565C0}.score-badge.road{background:#fff3e0;color:#E65100}
.dlg-stats{display:flex;gap:16px;margin-top:10px;font-size:12px;color:#999}.dlg-arrival{font-size:12px;color:#999;margin-top:6px}
</style>
