<template>
  <div class="home">
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
    <div class="hd"><h2>{{greeting}}</h2><p class="hd-sub">探索你的下一段旅程</p></div>
    <div class="acts">
      <div class="ac green" @click="$router.push('/generate')"><van-icon name="add-o" size="28"/><span>生成路书</span></div>
      <div class="ac blue" @click="$router.push('/record')"><van-icon name="location-o" size="28"/><span>记录行程</span></div>
    </div>
    <div class="sec">
      <div class="sec-head"><h3>我的路书 <small>({{routes.length}})</small></h3>
        <div class="sort-row">
          <van-search v-model="keyword" shape="round" placeholder="搜索路书" @search="onSearch" @clear="onSearch" class="home-search" />
          <span class="sort-toggle" v-if="routes.length>1" @click="cycleSort">{{sortLabel}} ⏷</span>
        </div>
        <div class="comp-bar" v-if="routes.length>=2">
          <van-button size="small" plain type="warning" @click="toggleCompare">{{compareMode?'取消对比':'对比路线'}}</van-button>
          <span v-if="compareMode" class="comp-hint">点击选择 2 条路线</span>
        </div>
      </div>
      <van-loading v-if="loading" class="sec-loading" />
      <van-empty v-else-if="!routes.length" image="search" description="还没有路书">
        <van-button round type="primary" size="small" to="/generate">去生成一条</van-button>
      </van-empty>
      <van-cell v-for="r in routes" :key="r.id" :title="r.title" :label="(r.status===2?'🔴 记录中 · ':'')+r.totalDays+'天 · '+(r.totalDistance||0)+'km · '+formatDate(r.createdAt)" is-link @click="onRouteClick(r)">
        <template #icon><span class="route-icon">{{r.status===2?'📍':'🗺️'}}</span></template>
        <template #right-icon v-if="compareMode">
          <van-checkbox :model-value="isSelected(r.id)" @click.stop="toggleSelect(r.id)" />
        </template>
      </van-cell>
    </div>
    <div class="sec">
      <h3>热门路线模板</h3>
      <div class="hs">
        <div v-for="t in hotRoutes" :key="t.id" class="hc" @click="previewTemplate(t)">
          <div class="hc-img">{{t.region.charAt(0)}}</div>
          <div class="hn">{{t.name}}</div>
          <div class="hm">{{t.region}} · {{t.totalDays}}天</div>
        </div>
      </div>
    </div>
    </van-pull-refresh>

    <van-overlay :show="showGuide">
      <div class="guide">
        <div class="guide-inner">
          <div class="guide-steps">
            <span v-for="i in 3" :key="i" class="gstep-dot" :class="{on: guideStep===i}"></span>
          </div>
          <template v-if="guideStep===1">
            <h3>🗺️ AI 智能规划</h3>
            <p>点击「<b>生成路书</b>」，选择起终点和天数，AI 自动为你规划最佳自驾路线</p>
          </template>
          <template v-else-if="guideStep===2">
            <h3>📍 记录沿途点滴</h3>
            <p>点击「<b>记录行程</b>」，在旅途中随时记录位置、拍照、写备注</p>
          </template>
          <template v-else>
            <h3>🚗 驾驶看板</h3>
            <p>切换到「<b>驾驶</b>」标签，查看实时位置、续航和周边补给</p>
          </template>
          <div class="guide-actions">
            <span v-if="guideStep>1" class="gprev" @click="guideStep--">上一步</span>
            <span v-else></span>
            <van-button round type="primary" size="small" @click="guideStep<3?guideStep++:closeGuide()">{{guideStep<3?'下一步 →':'开始探索'}}</van-button>
          </div>
        </div>
      </div>
    </van-overlay>

    <van-dialog v-model:show="showPreview" :title="previewTpl?.name" confirm-button-text="查看详情" @confirm="goTemplateDetail">
      <div class="tpl-preview" v-if="previewTpl">
        <p><b>区域:</b> {{previewTpl.region}}</p>
        <p><b>天数:</b> {{previewTpl.totalDays}} 天</p>
        <p><b>距离:</b> {{previewTpl.totalDistance||'约'}} km</p>
        <p><b>最佳季节:</b> {{previewTpl.bestSeason||'全年'}}</p>
        <p><b>难度:</b> {{previewTpl.difficulty===1?'轻松':previewTpl.difficulty===2?'中等':'挑战'}}</p>
      </div>
    </van-dialog>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'; import { useRouter } from 'vue-router'; import api from '../utils/api'
const router = useRouter()
const routes=ref([]), hotRoutes=ref([]), refreshing=ref(false), loading=ref(true)
const sortBy=ref('createdAt'), sortDir=ref('desc'), sortIdx=ref(0), keyword=ref('')
const sortLabels=['最新','最早','最长','最短','天数多','天数少']
const sortConfigs=[{by:'createdAt',dir:'desc'},{by:'createdAt',dir:'asc'},{by:'totalDistance',dir:'desc'},{by:'totalDistance',dir:'asc'},{by:'totalDays',dir:'desc'},{by:'totalDays',dir:'asc'}]
const sortLabel=computed(()=>sortLabels[sortIdx.value]||'最新')
function cycleSort(){sortIdx.value=(sortIdx.value+1)%sortLabels.length;const c=sortConfigs[sortIdx.value];sortBy.value=c.by;sortDir.value=c.dir;fetchRoutes()}
function onSearch(){fetchRoutes()}
const showPreview=ref(false), previewTpl=ref(null), showGuide=ref(!localStorage.getItem('guide_seen')), guideStep=ref(1)

const greeting=computed(()=>{const h=new Date().getHours();return h<12?'☀️ 早上好':h<18?'🌤️ 下午好':'🌙 晚上好'})

function formatDate(d){if(!d)return'';const t=new Date(d);return t.getFullYear()+'-'+(t.getMonth()+1)+'-'+t.getDate()}

async function fetchRoutes(){
  try{const params={size:20,sortBy:sortBy.value,sortDir:sortDir.value};if(keyword.value)params.keyword=keyword.value;const r=await api.get('/routes',{params});routes.value=r.data.data?.content||[]}catch{}
}
async function fetchAll(){await fetchRoutes()
  try{const r=await api.get('/templates/popular',{params:{limit:6}});hotRoutes.value=r.data.data||[]}catch{}
  loading.value=false
}
onMounted(fetchAll)
async function onRefresh(){refreshing.value=true;await fetchAll();refreshing.value=false}

function previewTemplate(t){previewTpl.value=t;showPreview.value=true}
function goTemplateDetail(){if(previewTpl.value){router.push('/route/'+previewTpl.value.id)}}
function closeGuide(){showGuide.value=false;localStorage.setItem('guide_seen','1')}

const compareMode=ref(false), selectedIds=ref([])
function toggleCompare(){compareMode.value=!compareMode.value;selectedIds.value=[]}
function toggleSelect(id){const i=selectedIds.value.indexOf(id);if(i>=0)selectedIds.value.splice(i,1);else if(selectedIds.value.length<2)selectedIds.value.push(id);if(selectedIds.value.length===2){sessionStorage.setItem('compare_ids',JSON.stringify(selectedIds.value));router.push('/compare')}}
function isSelected(id){return selectedIds.value.includes(id)}
function onRouteClick(r){if(compareMode.value)toggleSelect(r.id);else if(r.status===2)router.push('/record?id='+r.id);else router.push('/route/'+r.id)}
</script>
<style scoped>
.home{padding:12px;padding-bottom:60px}
.hd{padding:20px 8px 12px}.hd h2{margin:0;font-size:24px;color:#333}.hd-sub{color:#999;font-size:13px;margin:4px 0 0}
.acts{display:flex;gap:12px;margin:16px 0}
.ac{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:22px;border-radius:14px;color:#fff;gap:8px;cursor:pointer;transition:transform .15s}
.ac:active{transform:scale(.97)}
.ac.green{background:linear-gradient(135deg,#66BB6A,#2E7D32)}.ac.blue{background:linear-gradient(135deg,#42A5F5,#1565C0)}
.sec{margin-top:20px}.sec-head{margin-bottom:10px}.sec h3{font-size:16px;color:#333;margin:0 0 6px}.sec h3 small{color:#999;font-weight:400}.sort-row{display:flex;align-items:center;gap:8px}.home-search{flex:1;padding:0!important;--van-search-padding:0}.home-search :deep(.van-search__content){background:#f5f5f5;border-radius:18px}.sort-toggle{flex-shrink:0;font-size:12px;color:#2E7D32;cursor:pointer;white-space:nowrap}
.sec-loading{display:flex;justify-content:center;padding:30px}
.route-icon{font-size:20px;margin-right:8px}
.hs{display:flex;gap:10px;overflow-x:auto;padding-bottom:8px}
.hc{flex-shrink:0;width:130px;padding:0 0 12px;border-radius:12px;background:#fff;box-shadow:0 2px 10px rgba(0,0,0,.06);cursor:pointer;overflow:hidden}
.hc-img{height:70px;background:linear-gradient(135deg,#C8E6C9,#A5D6A7);display:flex;align-items:center;justify-content:center;font-size:32px;font-weight:700;color:#2E7D32;margin-bottom:8px}
.hn{font-size:13px;font-weight:600;color:#333;padding:0 10px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.hm{font-size:11px;color:#999;padding:2px 10px 0}
</style>
