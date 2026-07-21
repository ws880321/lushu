<template>
  <div class="tl"><div class="tc"><router-view /></div>
  <van-tabbar v-model="active" route>
    <van-tabbar-item icon="home-o" to="/home">路书</van-tabbar-item>
    <van-tabbar-item icon="add-o" to="/generate">生成</van-tabbar-item>
    <van-tabbar-item icon="friends-o" to="/community">社区</van-tabbar-item>
    <van-tabbar-item icon="chart-trending-o" to="/dashboard">驾驶</van-tabbar-item>
    <van-tabbar-item icon="user-o" to="/profile">我的</van-tabbar-item>
  </van-tabbar></div>
</template>
<script setup>
import { ref, watch } from 'vue'; import { useRoute } from 'vue-router'
const route = useRoute(); const active = ref(0)
watch(()=>route.meta.tab, v=>{if(v!==undefined)active.value=v},{immediate:true})
</script>
<style scoped>.tl{display:flex;flex-direction:column;min-height:100vh}.tc{flex:1;padding-bottom:50px}</style>
