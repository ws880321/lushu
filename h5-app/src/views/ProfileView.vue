<template>
  <div class="pf">
    <div class="pf-head"><div class="pf-avatar">🚗</div><div class="pf-info"><h3>自驾用户</h3><p>{{member===2?'⭐ 终身会员':member===1?'💎 Pro会员':'我的自驾路书'}}</p></div></div>
    <div class="member-card" @click="showPlans=true"><div class="mc-left">💎 {{memberName}}</div><div class="mc-right">{{memberDesc}}<van-icon name="arrow" style="margin-left:4px"/></div></div>
    <van-cell-group inset>
      <van-cell title="我的车辆" :value="vehName" is-link @click="showVeh=true" />
      <van-cell title="总行程" :value="stats.trips+' 次'" />
      <van-cell title="累计里程" :value="stats.distance+' km'" />
    </van-cell-group>
    <div class="pf-actions"><van-button round block type="danger" @click="handleLogout">退出登录</van-button></div>
    <van-dialog v-model:show="showPlans" title="升级会员" show-cancel-button><div class="plans"><div v-for="p in plans" :key="p.id" class="plan" :class="{sel:member===p.id}" @click="doUpgrade(p)"><div class="plan-top"><span class="plan-name">{{p.name}}</span><span class="plan-price">{{p.price===0?'免费':'¥'+p.price}}</span></div><div class="plan-features" v-for="f in (p.features||'').split('|')" :key="f">{{f}}</div></div></div></van-dialog>

    <van-dialog v-model:show="showVeh" title="车辆信息" @confirm="saveVehicle" show-cancel-button>
      <van-field v-model="vname" label="车名" placeholder="如: 我的SUV" />
      <van-field v-model="vbrand" label="品牌" placeholder="如: 丰田普拉多" />
      <van-field v-model="vrange" type="number" label="续航(km)" placeholder="如: 600" />
      <van-field label="燃油类型">
        <template #input>
          <van-radio-group v-model="fuelCode" direction="horizontal">
            <van-radio name="gas">汽油</van-radio>
            <van-radio name="diesel">柴油</van-radio>
            <van-radio name="electric">电动</van-radio>
          </van-radio-group>
        </template>
      </van-field>
    </van-dialog>
  </div>
</template>
<script setup>
import { ref, onMounted, computed } from 'vue'; import { useRouter } from 'vue-router'; import { showToast } from 'vant'
import api from '../utils/api'
const router = useRouter()
const vehName=ref('未设置'),vehId=ref(null),showVeh=ref(false),showPlans=ref(false)
const vname=ref(''),vbrand=ref(''),vrange=ref(''),fuelCode=ref('gas')
const stats=ref({trips:0,distance:0}),member=ref(0),plans=ref([])
const memberName=computed(()=>member.value===2?'终身会员':member.value===1?'Pro会员':'免费用户')
const memberDesc=computed(()=>member.value>0?'已订阅':'点击升级 →')

onMounted(async()=>{
  try{const r=await api.get('/routes',{params:{size:1000}});const list=r.data.data?.content||[];stats.value.trips=list.length;stats.value.distance=list.reduce((s,r)=>s+(r.totalDistance||0),0)}catch{}
  try{const r=await api.get('/vehicles');const vs=r.data.data;if(vs&&vs.length){const v=vs[0];vehName.value=v.name||v.brand||'车辆';vehId.value=v.id;vname.value=v.name||'';vbrand.value=v.brand||'';vrange.value=v.rangeFull||'';fuelCode.value=v.fuelType||'gas'}}catch{}
  try{const r=await api.get('/member/usage');member.value=r.data.data.membership||0}catch{}
  try{const r=await api.get('/member/plans');plans.value=r.data.data||[]}catch{}
})
async function saveVehicle(){
  try{
    const p={name:vname.value,brand:vbrand.value,fuelType:fuelCode.value,rangeFull:parseInt(vrange.value)||0}
    if(vehId.value){await api.put('/vehicles/'+vehId.value,p)}else{const r=await api.post('/vehicles',p);vehId.value=r.data.data.id}
    vehName.value=vname.value||'车辆';showVeh.value=false;showToast('已保存')
  }catch{showToast('保存失败')}
}
async function doUpgrade(p){if(p.id===member.value){showToast('已是当前套餐');return};try{await api.post('/member/upgrade',{planId:p.id});member.value=p.id;showPlans.value=false;showToast('已升级')}catch{showToast('升级失败')}}
function handleLogout(){localStorage.removeItem('app_token');router.replace('/login')}
</script>
<style scoped>
.pf{padding:12px}.pf-head{display:flex;align-items:center;gap:16px;padding:24px 20px;background:linear-gradient(135deg,#4CAF50,#2E7D32);border-radius:12px;color:#fff;margin-bottom:16px}
.member-card{display:flex;justify-content:space-between;align-items:center;padding:14px 16px;margin:0 0 12px;background:linear-gradient(135deg,#FDED72,#F5D76E);border-radius:10px;cursor:pointer;box-shadow:0 1px 4px rgba(0,0,0,.06)}.mc-left{font-size:15px;font-weight:600}.mc-right{font-size:12px;color:#666;display:flex;align-items:center}
.plans{padding:8px 16px 16px;display:flex;flex-direction:column;gap:8px}.plan{padding:10px;border-radius:8px;border:1px solid #eee;cursor:pointer}.plan.sel{border-color:#2E7D32;background:#f1f8e9}.plan-top{display:flex;justify-content:space-between;font-weight:600;margin-bottom:2px}.plan-name{font-size:14px}.plan-price{color:#2E7D32}.plan-features{font-size:11px;color:#999;margin-top:1px}
.pf-avatar{font-size:48px}.pf-info h3{margin:0 0 4px;font-size:20px}
.pf-actions{margin-top:32px;padding:0 12px}
</style>
