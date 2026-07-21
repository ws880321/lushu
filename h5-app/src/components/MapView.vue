<template>
  <div ref="mapEl" class="map-wrap" :style="{height}"></div>
</template>
<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
const props = defineProps({ height:{default:'300px'}, center:{type:Array,default:()=>[104,30]}, zoom:{default:10}, markers:{type:Array,default:()=>[]}, polylines:{type:Array,default:()=>[]} })
const emit = defineEmits(['markerClick'])
const mapEl = ref(null); let map = null
const KEY = 'df1e5cca15cf847eeae3e94060be2525'
function loadAMap(){return new Promise((rs,rj)=>{if(window.AMap)return rs(window.AMap);const s=document.createElement('script');s.src=`https://webapi.amap.com/maps?v=2.0&key=${KEY}`;s.onload=()=>rs(window.AMap);s.onerror=rj;document.head.appendChild(s)})}
onMounted(async()=>{await nextTick();const AM=await loadAMap();map=new AM.Map(mapEl.value,{zoom:props.zoom,center:props.center});renderAll()})
function renderAll(){if(!map)return;map.clearMap();props.markers.forEach(m=>{const mk=new AMap.Marker({position:m.position,label:m.label});mk.on('click',()=>emit('markerClick',m));map.add(mk)});props.polylines.forEach(p=>{map.add(new AMap.Polyline(p))})}
watch(()=>[props.markers,props.polylines],()=>renderAll(),{deep:true})
onUnmounted(()=>{if(map){map.destroy();map=null}})
defineExpose({getMap:()=>map})
</script>
<style scoped>.map-wrap{border-radius:8px;overflow:hidden}</style>
