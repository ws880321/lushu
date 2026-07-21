<template>
  <div class="cv">
    <van-nav-bar title="路线对比" left-text="返回" left-arrow @click-left="$router.back()" />
    <van-empty v-if="!routeA||!routeB" description="请从路线列表页面选择两条路线" image="search">
      <van-button round type="primary" size="small" to="/home">去选择</van-button>
    </van-empty>
    <template v-else>
      <div class="cv-grid">
        <div class="cv-col">
          <div class="cv-label">路线 A</div>
          <div class="cv-name">{{ routeA.title }}</div>
        </div>
        <div class="cv-col">
          <div class="cv-label">路线 B</div>
          <div class="cv-name">{{ routeB.title }}</div>
        </div>
      </div>
      <div class="cv-grid" v-for="(row,i) in compRows" :key="i">
        <div class="cv-col" :class="row.aBetter?'better':(row.bBetter?'worse':'' )">
          <div class="cv-val">{{ row.aVal }}</div>
        </div>
        <div class="cv-mid">{{ row.label }}</div>
        <div class="cv-col" :class="row.bBetter?'better':(row.aBetter?'worse':'' )">
          <div class="cv-val">{{ row.bVal }}</div>
        </div>
      </div>
      <div class="cv-verdict" v-if="verdict">
        <van-tag type="primary" size="large">{{ verdict }}</van-tag>
      </div>
    </template>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'; import { useRouter } from 'vue-router'
import api from '../utils/api'
const router = useRouter()
const routeA=ref(null), routeB=ref(null)

onMounted(async()=>{
  const ids=JSON.parse(sessionStorage.getItem('compare_ids')||'[]')
  if(ids.length===2){
    try{const r=await Promise.all([api.get('/routes/'+ids[0]),api.get('/routes/'+ids[1])]);routeA.value=r[0].data.data;routeB.value=r[1].data.data}
    catch{routeA.value=null}
  }
  sessionStorage.removeItem('compare_ids')
})

const compRows=computed(()=>{
  const a=routeA.value,b=routeB.value;if(!a||!b)return[]
  const aKm=a.totalDistanceKm||0,bKm=b.totalDistanceKm||0
  const aCost=a.estimatedCost?.totalYuan||0,bCost=b.estimatedCost?.totalYuan||0
  const aDays=a.totalDays||0,bDays=b.totalDays||0
  const aWp=countWps(a),bWp=countWps(b)
  const aCostPerDay=aDays>0?Math.round(aCost/aDays):0,bCostPerDay=bDays>0?Math.round(bCost/bDays):0
  return [
    {label:'总天数',aVal:aDays+'天',bVal:bDays+'天'},
    {label:'总里程',aVal:aKm+'km',bVal:bKm+'km'},
    {label:'总费用',aVal:'¥'+aCost,bVal:'¥'+bCost,aBetter:aCost<bCost,bBetter:bCost<aCost},
    {label:'日均费用',aVal:'¥'+aCostPerDay,bVal:'¥'+bCostPerDay,aBetter:aCostPerDay<bCostPerDay,bBetter:bCostPerDay<aCostPerDay},
    {label:'途经点',aVal:aWp+'个',bVal:bWp+'个'},
    {label:'起点',aVal:a.startPoint||'?',bVal:b.startPoint||'?'},
    {label:'终点',aVal:a.endPoint||'?',bVal:b.endPoint||'?'},
  ]
})

const verdict=computed(()=>{
  const a=routeA.value,b=routeB.value;if(!a||!b)return''
  const aCost=a.estimatedCost?.totalYuan||0,bCost=b.estimatedCost?.totalYuan||0
  const aDays=a.totalDays||0,bDays=b.totalDays||0
  let score=0
  if(aCost<bCost)score++;else if(aCost>bCost)score--
  if(aDays<bDays)score++;else if(aDays>bDays)score--
  if(score>0)return '🏆 A 更优 — 费用更低天数更少'
  if(score<0)return '🏆 B 更优 — 费用更低天数更少'
  return '⚖️ 各有优势，根据需要选择'
})

function countWps(route){
  if(!route?.itinerary)return 0
  return Object.values(route.itinerary).reduce((sum,wps)=>sum+(Array.isArray(wps)?wps.length:0),0)
}
</script>
<style scoped>
.cv{padding:0 0 40px}
.cv-grid{display:flex;align-items:stretch;margin:0 12px 8px}
.cv-col{flex:1;background:#fff;padding:12px;text-align:center;border-radius:8px;box-shadow:0 1px 4px rgba(0,0,0,.04)}
.cv-mid{width:60px;display:flex;align-items:center;justify-content:center;font-size:12px;color:#999;font-weight:600}
.cv-label{font-size:11px;color:#999;margin-bottom:4px}
.cv-name{font-size:14px;font-weight:600;color:#333}
.cv-val{font-size:16px;font-weight:700;color:#333}
.cv-col.better{background:#e8f5e9;border:1px solid #A5D6A7}.cv-col.worse{background:#fff3e0;border:1px solid #FFCC80}
.cv-col.better .cv-val{color:#2E7D32}.cv-col.worse .cv-val{color:#E65100}
.cv-verdict{text-align:center;padding:20px}
</style>
