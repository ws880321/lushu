<template>
  <div class="cm">
    <div class="cm-hd"><h3>🏘️ 探索路书</h3><p>发现大家分享的自驾路线</p></div>

    <!-- Collections -->
    <div class="cols-scroll">
      <div v-for="c in collections" :key="c.id" class="col-card" @click="$router.push('/generate')">
        <div class="col-cover">{{c.cover}}</div>
        <div class="col-name">{{c.name}}</div>
        <div class="col-desc">{{c.desc}}</div>
      </div>
    </div>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <div v-for="r in routes" :key="r.id" class="cm-card" @click="$router.push('/route/'+r.id)">
        <div class="cm-cover">{{r.region||'自驾'}}</div>
        <div class="cm-info">
          <div class="cm-title">{{r.title}}</div>
          <div class="cm-meta">{{r.totalDays}}天 · {{r.totalDistance||0}}km · {{r.startPoint}} → {{r.endPoint}}</div>
          <div class="cm-date">{{fmtDate(r.createdAt)}}</div>
        </div>
      </div>
      <van-empty v-if="!loading&&!routes.length" description="还没有人分享路书" />
    </van-pull-refresh>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'; import api from '../utils/api'
const routes=ref([]),collections=ref([]),refreshing=ref(false),loading=ref(true)
async function fetchAll(){try{const r=await api.get('/routes/community',{params:{size:20}});routes.value=r.data.data||[]}catch{};loading.value=false}
function fmtDate(d){return d?new Date(d).toLocaleDateString('zh-CN'):''}
onMounted(async()=>{await fetchAll();try{const r=await api.get('/collections');collections.value=r.data.data||[]}catch{}})
async function onRefresh(){refreshing.value=true;await fetchAll();refreshing.value=false}
</script>
<style scoped>
.cm{padding:12px;padding-bottom:60px}.cm-hd{padding:8px}.cm-hd h3{margin:0}.cm-hd p{color:#999;font-size:13px;margin:4px 0 0}
.cols-scroll{display:flex;gap:10px;overflow-x:auto;padding:8px 0 12px}
.col-card{flex-shrink:0;width:130px;padding:12px;background:#fff;border-radius:12px;box-shadow:0 2px 8px rgba(0,0,0,.06);cursor:pointer;text-align:center}
.col-cover{font-size:32px;margin-bottom:6px}.col-name{font-size:13px;font-weight:600;color:#333}.col-desc{font-size:11px;color:#999;margin-top:4px;line-height:1.4}
.cm-card{display:flex;gap:12px;padding:12px;background:#fff;border-radius:10px;margin-bottom:10px;box-shadow:0 1px 4px rgba(0,0,0,.06);cursor:pointer}
.cm-cover{width:70px;height:70px;border-radius:8px;background:linear-gradient(135deg,#C8E6C9,#81C784);display:flex;align-items:center;justify-content:center;font-size:22px;font-weight:700;color:#2E7D32;flex-shrink:0}
.cm-info{flex:1;display:flex;flex-direction:column;justify-content:center}.cm-title{font-size:15px;font-weight:600;color:#333}.cm-meta{font-size:12px;color:#999;margin:4px 0}.cm-date{font-size:11px;color:#bbb}
</style>
