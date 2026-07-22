<template>
  <div class="gen">
    <div class="hd"><h3>生成自驾路书</h3></div>

    <!-- Form (hidden when results shown) -->
    <template v-if="!showResults">
    <div class="card">
      <div class="card-title">起终点 & 天数</div>
      <van-field v-model="startText" placeholder="起点（城市或地址）" label="起点">
        <template #extra>
          <van-button size="mini" plain type="primary" @click="openCityPick('start')">省市</van-button>
          <van-button size="mini" plain type="default" @click="openMapPick('start')">地图</van-button>
        </template>
      </van-field>
      <van-field v-model="endText" placeholder="终点（留空则同起点）" label="终点">
        <template #extra>
          <van-button size="mini" plain type="primary" @click="openCityPick('end')">省市</van-button>
          <van-button size="mini" plain type="default" @click="openMapPick('end')">地图</van-button>
        </template>
      </van-field>
      <div class="stepper-row"><span>天数</span><van-stepper v-model="totalDays" :min="1" :max="15" /></div>
    </div>

    <div class="card">
      <div class="card-title">偏好设置</div>
      <div class="row-label">难度</div>
      <div class="pills">
        <span v-for="d in difficulties" :key="d.value" class="pill" :class="{active:difficulty===d.value}" @click="difficulty=d.value">{{d.label}}</span>
      </div>
      <div class="row-label">兴趣标签 <small>({{selectedTags.length}}/8)</small></div>
      <div class="pills">
        <span v-for="t in allTags" :key="t" class="pill" :class="{active:selectedTags.includes(t)}" @click="toggleTag(t)">{{t}}</span>
      </div>
      <div class="row-label">每日驾驶: {{dailyHours}}小时</div>
      <van-slider v-model="dailyHours" :min="2" :max="8" :step="0.5" active-color="#2E7D32" />
    </div>

    <div class="card">
      <div class="card-title">热门路线模板</div>
      <div class="hot-scroll">
        <div v-for="t in hotTemplates" :key="t.id" class="hot-card" @click="fillTemplate(t)">
          <div class="hot-name">{{t.name}}</div>
          <small>{{t.region}} · {{t.totalDays}}天 · {{dLabel(t.difficulty)}}</small>
        </div>
      </div>
    </div>

    <van-button round block type="primary" :loading="generating" @click="onGenerate" class="gen-btn">
      {{ generating ? 'AI 正在规划路线...' : '生成路线' }}
    </van-button>
    </template>

    <!-- Results: route selection -->
    <div v-if="showResults" class="results">
      <div class="results-hd">
        <h3>🎉 为您规划了 {{allRoutes.length}} 条路线</h3>
        <p class="results-sub">点击选择一条查看详情</p>
      </div>

      <div v-for="(r, i) in allRoutes" :key="r.routeId"
           class="route-card" :class="{primary: i===0 && aiGenerated}"
           @click="selectRoute(r)">
        <div class="rc-badge" v-if="i===0 && aiGenerated">🤖 AI 推荐</div>
        <div class="rc-badge tpl" v-else-if="i===0">⭐ 最佳匹配</div>
        <div class="rc-title">{{r.title}}</div>
        <div class="rc-stats">
          <span>{{r.totalDays}}天</span>
          <span>{{r.totalDistanceKm || '?'}}km</span>
          <span v-if="r.estimatedCost">¥{{Math.round(r.estimatedCost.totalYuan)}}</span>
        </div>
        <div class="rc-desc" v-if="r.description">{{r.description}}</div>
      </div>

      <van-button round block plain @click="showResults=false" class="regen-btn">重新生成</van-button>
    </div>

    <!-- City Picker (native overlay, no Vant popup deps) -->
    <div v-if="showPicker" class="picker-overlay" @click.self="showPicker=false">
      <div class="picker-panel">
        <div class="picker-bar">
          <span @click="showPicker=false">取消</span>
          <span class="pbar-title">{{pickTarget==='start'?'起点':'终点'}}</span>
          <span class="pbar-ok" @click="onCityPickConfirm">确定</span>
        </div>
        <div class="picker-body">
          <div class="pk-col">
            <div class="pk-title">省份</div>
            <div class="pk-list">
              <div v-for="(p,i) in provinces" :key="p" class="pk-item" :class="{on:i===provIdx}" @click="provIdx=i;cityIdx=0">{{p}}</div>
            </div>
          </div>
          <div class="pk-col">
            <div class="pk-title">城市</div>
            <div class="pk-list">
              <div v-for="(c,i) in currentCities" :key="c" class="pk-item" :class="{on:i===cityIdx}" @click="cityIdx=i">{{c}}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Map Picker Popup -->
    <van-popup v-model:show="showMapPicker" position="bottom" :style="{ height: '60vh' }" round>
      <div class="map-picker-header">
        <span>{{mapPickTarget==='start'?'选择起点':'选择终点'}}</span>
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
import { ref, computed, onMounted } from 'vue'; import { useRouter } from 'vue-router'; import { showToast } from 'vant'
import api from '../utils/api'; import { cacheRoute } from '../utils/cache'
import { reverseGeocode } from '../utils/amap'
import MapPicker from '../components/MapPicker.vue'
const router = useRouter()
const startText=ref(''), endText=ref(''), startLng=ref(null), startLat=ref(null), endLng=ref(null), endLat=ref(null)
const totalDays=ref(3), difficulty=ref('medium'), selectedTags=ref([]), dailyHours=ref(4), generating=ref(false)
const difficulties=[{label:'轻松',value:'easy'},{label:'中等',value:'medium'},{label:'挑战',value:'hard'}]
const allTags=['📷 摄影','⛺ 露营','🌿 自然','🏔 雪山','🍜 美食','🏛 人文','🚗 越野','👨‍👩‍👧 亲子']

function toggleTag(t){ const i=selectedTags.value.indexOf(t); i>=0?selectedTags.value.splice(i,1):selectedTags.value.length<8&&selectedTags.value.push(t) }
function dLabel(v){ return v===1?'轻松':v===2?'中等':v===3?'挑战':'' }

const hotTemplates=ref([]), showPicker=ref(false), pickTarget=ref('start'), provIdx=ref(0), cityIdx=ref(0)
const showMapPicker=ref(false), mapPickTarget=ref('start'), mapPickerCoords=ref(null)
const showResults=ref(false), generatedRoutes=ref([]), aiGenerated=ref(false)
const allRoutes=computed(()=>generatedRoutes.value)
const provinces=['四川','云南','甘肃','青海','陕西','贵州','浙江','安徽','宁夏','西藏','新疆','黑龙江','海南','福建']
const allCities={四川:['成都','绵阳','乐山','康定','雅安','泸州'],云南:['昆明','大理','丽江','香格里拉'],甘肃:['兰州','甘南','嘉峪关'],青海:['西宁','海东'],陕西:['西安','宝鸡'],贵州:['贵阳','遵义','安顺'],浙江:['杭州','温州'],安徽:['合肥','黄山'],宁夏:['银川'],西藏:['拉萨'],新疆:['乌鲁木齐'],黑龙江:['哈尔滨'],海南:['海口','三亚'],福建:['福州','厦门']}
const coords={成都:[104.07,30.66],绵阳:[104.68,31.47],乐山:[103.77,29.55],康定:[101.96,30.05],雅安:[103.02,29.98],泸州:[105.44,28.87],昆明:[102.71,25.04],大理:[100.23,25.59],丽江:[100.23,26.87],香格里拉:[99.71,27.83],兰州:[103.83,36.06],甘南:[102.92,34.99],嘉峪关:[98.29,39.77],西宁:[101.78,36.62],海东:[102.10,36.50],西安:[108.94,34.26],宝鸡:[107.24,34.36],贵阳:[106.63,26.65],遵义:[106.93,27.73],安顺:[105.95,26.25],杭州:[120.16,30.27],温州:[120.70,27.99],合肥:[117.23,31.82],黄山:[118.17,30.27],银川:[106.23,38.49],拉萨:[91.11,29.66],乌鲁木齐:[87.62,43.79],哈尔滨:[126.64,45.76],海口:[110.20,20.04],三亚:[109.51,18.25],福州:[119.31,26.08],厦门:[118.09,24.48]}
const currentCities=computed(()=>allCities[provinces[provIdx.value]]||['选择城市'])

onMounted(async()=>{try{const r=await api.get('/templates/popular',{params:{limit:20}});hotTemplates.value=r.data.data||[]}catch{}})

function openCityPick(t){ pickTarget.value=t; provIdx.value=0; cityIdx.value=0; showPicker.value=true }
function onCityPickConfirm(){ const prov=provinces[provIdx.value]; const cities=currentCities.value; const city=cities[cityIdx.value]; const c=coords[city]; if(pickTarget.value==='start'){ startText.value=city||prov; if(c){startLng.value=c[0];startLat.value=c[1]} }else{ endText.value=city||prov; if(c){endLng.value=c[0];endLat.value=c[1]} } showPicker.value=false }

function fillTemplate(t){ startText.value=t.region; totalDays.value=t.totalDays }

function openMapPick(t){ mapPickTarget.value=t; mapPickerCoords.value=null; showMapPicker.value=true }

async function onMapPickConfirm(){
  if(!mapPickerCoords.value) return
  const c=mapPickerCoords.value
  showMapPicker.value=false
  if(mapPickTarget.value==='start'){ startLng.value=c.lng; startLat.value=c.lat } else { endLng.value=c.lng; endLat.value=c.lat }
  try{
    const addr=await reverseGeocode(c.lng,c.lat)
    if(!addr.match(/^\d+\.\d+/)){ if(mapPickTarget.value==='start') startText.value=addr; else endText.value=addr }
    else { const label=mapPickTarget.value==='start'?'起点':'终点'; if(mapPickTarget.value==='start') startText.value=label; else endText.value=label }
  } catch {
    const label=mapPickTarget.value==='start'?'起点':'终点'; if(mapPickTarget.value==='start') startText.value=label; else endText.value=label
  }
}
async function onGenerate(){
  if(!startText.value) return showToast('请输入起点')
  generating.value=true
  try{
    const payload={totalDays:totalDays.value,startPoint:{name:startText.value,lng:startLng.value,lat:startLat.value},endPoint:{name:endText.value||startText.value,lng:endLng.value,lat:endLat.value},preferences:{difficulty:difficulty.value,tags:selectedTags.value,dailyDriveHours:dailyHours.value}}
    const r=await api.post('/routes/generate',payload)
    if(r.data.code===40401){ showToast(r.data.message||'暂无匹配路线，请调整条件'); return }
    const data=r.data.data
    const all=[]
    if(data.primary) all.push(data.primary)
    if(data.alternatives) all.push(...data.alternatives)
    generatedRoutes.value=all; aiGenerated.value=data.aiGenerated||false
    all.forEach(rt=>cacheRoute(rt.routeId,rt))
    showResults.value=true
  }catch(e){ showToast(e.response?.data?.message||'生成失败，请稍后重试') }
  finally{ generating.value=false }
}
function selectRoute(r){ cacheRoute(r.routeId,r); router.push('/route/'+r.routeId) }
</script>
<style scoped>
.gen{padding:12px;padding-bottom:80px}.hd{padding:8px}.hd h3{margin:0}
.card{background:#fff;border-radius:10px;padding:14px;margin-bottom:12px;box-shadow:0 1px 4px rgba(0,0,0,.06)}
.card-title{font-size:15px;font-weight:600;color:#333;margin-bottom:8px}
.stepper-row{display:flex;justify-content:space-between;align-items:center;padding:10px 0}
.row-label{font-size:13px;color:#666;margin:8px 0 4px}
.pills{display:flex;gap:8px;flex-wrap:wrap}
.pill{padding:6px 14px;border-radius:16px;background:#f0f0f0;font-size:13px;cursor:pointer}.pill.active{background:#2E7D32;color:#fff}
.hot-scroll{display:flex;gap:8px;overflow-x:auto}.hot-card{flex-shrink:0;padding:10px 14px;border-radius:8px;background:#f5f5f5;cursor:pointer;text-align:center;font-size:13px}.hot-card small{color:#999}
.gen-btn{margin-top:12px;background:#2E7D32;border-color:#2E7D32}
/* City Picker Overlay */
.picker-overlay{position:fixed;inset:0;z-index:9999;background:rgba(0,0,0,.4);display:flex;align-items:flex-end}
.picker-panel{width:100%;background:#fff;border-radius:16px 16px 0 0;max-height:60vh;display:flex;flex-direction:column;animation:slideUp .25s}
@keyframes slideUp{from{transform:translateY(100%)}to{transform:translateY(0)}}
.picker-bar{display:flex;justify-content:space-between;align-items:center;padding:14px 16px;border-bottom:1px solid #eee}
.picker-bar span{font-size:15px;cursor:pointer}.pbar-title{font-weight:600;color:#333}
.pbar-ok{color:#2E7D32;font-weight:600}
.picker-body{display:flex;flex:1;overflow:hidden}
.pk-col{flex:1;display:flex;flex-direction:column}
.pk-col:first-child{border-right:1px solid #f0f0f0}
.pk-title{text-align:center;padding:8px;font-size:12px;color:#999;background:#fafafa}
.pk-list{flex:1;overflow-y:auto;-webkit-overflow-scrolling:touch}
.pk-item{padding:12px;text-align:center;font-size:15px;cursor:pointer}
.pk-item.on{background:#e8f5e9;color:#2E7D32;font-weight:600}

/* Map Picker */
.map-picker-header{display:flex;justify-content:space-between;align-items:center;padding:12px 16px;font-size:15px;font-weight:600}
.map-picker-footer{padding:12px 16px}

/* Route Selection Results */
.results{padding-bottom:40px}
.results-hd{text-align:center;padding:16px 0 8px}
.results-hd h3{margin:0;font-size:18px;color:#333}
.results-sub{color:#999;font-size:13px;margin:4px 0 0}
.route-card{position:relative;background:#fff;border-radius:12px;padding:16px;margin-bottom:12px;box-shadow:0 2px 8px rgba(0,0,0,.06);cursor:pointer;transition:transform .15s}
.route-card:active{transform:scale(.98)}
.route-card.primary{border:2px solid #4CAF50;background:linear-gradient(135deg,#f1f8e9,#fff)}
.rc-badge{display:inline-block;padding:3px 12px;border-radius:12px;font-size:12px;font-weight:600;margin-bottom:8px}
.rc-badge:not(.tpl){background:#4CAF50;color:#fff}
.rc-badge.tpl{background:#E8F5E9;color:#2E7D32}
.rc-title{font-size:16px;font-weight:600;color:#333;margin-bottom:6px}
.rc-stats{display:flex;gap:12px;font-size:13px;color:#666;margin-bottom:4px}
.rc-desc{font-size:12px;color:#999;margin-top:4px;line-height:1.5;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.regen-btn{margin-top:16px;color:#2E7D32;border-color:#2E7D32}
</style>
