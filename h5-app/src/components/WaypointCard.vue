<template>
  <div class="wpc">
    <div class="wpc-gutter"><div class="wpc-dot" :class="type"></div><div v-if="!isLast" class="wpc-line"></div></div>
    <div class="wpc-body">
      <div class="wpc-name">{{name}}</div>
      <div class="wpc-meta">
        <span class="wpc-type" :class="type">{{typeLabel}}</span>
        <span v-if="arrival" class="wpc-time">{{arrival}}<template v-if="departure"> → {{departure}}</template></span>
        <span v-if="stayMin" class="wpc-stay">🕐{{stayMin}}分钟</span>
      </div>
      <div v-if="tips" class="wpc-tips">{{tips}}</div>
      <div v-if="distanceFromPrevKm" class="wpc-dist">📏 距上站 {{distanceFromPrevKm}}km</div>
    </div>
  </div>
</template>
<script setup>
import { computed } from 'vue'
const props = defineProps({name:String,type:String,arrival:String,departure:String,tips:String,distanceFromPrevKm:Number,stayMin:Number,isLast:Boolean})
const TYPES={scenic:'景点',restaurant:'餐厅',hotel:'住宿',gas:'加油',rest:'休息',start:'出发',end:'终点',break:'休息点'}
const typeLabel = computed(()=>TYPES[props.type]||props.type)
</script>
<style scoped>
.wpc{display:flex;gap:12px;padding:8px 0}
.wpc-gutter{display:flex;flex-direction:column;align-items:center;width:20px;flex-shrink:0}
.wpc-dot{width:12px;height:12px;border-radius:50%;background:#2E7D32}.wpc-dot.start{background:#1976D2}.wpc-dot.end{background:#E53935}.wpc-dot.break{background:#FF9800}.wpc-dot.scenic{background:#4CAF50}.wpc-dot.restaurant{background:#FF9800}.wpc-dot.hotel{background:#9C27B0}.wpc-dot.gas{background:#E53935}
.wpc-line{flex:1;width:2px;background:#e0e0e0;min-height:20px}
.wpc-body{flex:1}.wpc-name{font-size:14px;font-weight:600;color:#333}.wpc-meta{display:flex;gap:8px;margin-top:4px;align-items:center}.wpc-type{font-size:11px;padding:1px 6px;border-radius:4px;background:#e8f5e9;color:#2E7D32}.wpc-type.restaurant{background:#fff3e0;color:#FF9800}.wpc-type.hotel{background:#f3e5f5;color:#9C27B0}.wpc-type.gas{background:#ffebee;color:#E53935}.wpc-time{font-size:12px;color:#999}.wpc-stay{font-size:11px;color:#FF9800}.wpc-tips{font-size:12px;color:#666;margin-top:4px;background:#f9f9f9;padding:6px 8px;border-radius:4px}.wpc-dist{font-size:11px;color:#999;margin-top:4px}
</style>
