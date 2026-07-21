<template>
  <div class="lp">
    <div class="lp-hero">
      <div class="lp-icon">🗺️</div>
      <h1>路书</h1>
      <p>你的自驾路书助手 · 让每一次出发都有方向</p>
    </div>
    <div class="lp-card">
      <van-field v-model="phone" type="tel" placeholder="请输入手机号" maxlength="11" :rules="[{required:true}]" @blur="validatePhone">
        <template #left-icon><van-icon name="phone-o" /></template>
      </van-field>
      <div class="lp-code">
        <van-field v-model="code" placeholder="验证码" maxlength="6" type="digit">
          <template #left-icon><van-icon name="shield-o" /></template>
        </van-field>
        <van-button :disabled="countdown>0" size="small" type="primary" plain @click="sendCode">
          {{ countdown>0 ? countdown+'s' : '获取验证码' }}
        </van-button>
      </div>
      <van-button round block type="primary" :loading="loading" @click="handleLogin" class="lp-btn">登 录</van-button>
      <p class="lp-hint">首次登录自动注册 · 验证码 000000</p>
    </div>
    <p class="lp-footer">登录即表示同意 <a>用户协议</a> 和 <a>隐私政策</a></p>
  </div>
</template>
<script setup>
import { ref } from 'vue'; import { useRouter } from 'vue-router'; import { useAuthStore } from '../stores/auth'; import { showToast, showFailToast } from 'vant'
const router = useRouter(); const auth = useAuthStore()
const phone=ref(''), code=ref(''), loading=ref(false), countdown=ref(0)
const PHONE_RE = /^1[3-9]\d{9}$/

function validatePhone(){ if(phone.value&&!PHONE_RE.test(phone.value)){ showToast('请输入正确的手机号') } }
async function sendCode(){
  if(!PHONE_RE.test(phone.value)) return showToast('请输入正确的手机号')
  try{await auth.sendCode(phone.value);showToast('验证码已发送');countdown.value=60;const t=setInterval(()=>{countdown.value--;if(countdown.value<=0)clearInterval(t)},1000)}catch(e){showFailToast(e.response?.data?.message||'发送失败，请稍后重试')}
}
async function handleLogin(){
  if(!PHONE_RE.test(phone.value)) return showToast('请输入正确的手机号')
  if(!code.value||code.value.length<6) return showToast('请输入6位验证码')
  loading.value=true
  try{await auth.login(phone.value,code.value,'自驾用户');router.replace('/home')}catch(e){showFailToast(e.response?.data?.message||'登录失败，请检查手机号和验证码')}finally{loading.value=false}
}
</script>
<style scoped>
.lp{min-height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:24px;background:linear-gradient(160deg,#e8f5e9 0%,#f1f8e9 50%,#e3f2fd 100%)}
.lp-hero{text-align:center;margin-bottom:36px}.lp-icon{font-size:64px}.lp-hero h1{font-size:36px;color:#2E7D32;margin:8px 0}.lp-hero p{color:#888;font-size:14px;margin:0}
.lp-card{width:100%;max-width:360px;background:#fff;border-radius:16px;padding:28px 24px;box-shadow:0 4px 24px rgba(0,0,0,.06)}
.lp-code{display:flex;align-items:center;gap:8px}.lp-code .van-field{flex:1}
.lp-btn{margin-top:24px;background:#2E7D32;border-color:#2E7D32;height:46px;font-size:16px}
.lp-hint{text-align:center;color:#bbb;font-size:12px;margin:14px 0 0}
.lp-footer{text-align:center;color:#ccc;font-size:11px;margin-top:32px}.lp-footer a{color:#2E7D32}
</style>
