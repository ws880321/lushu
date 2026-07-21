<template>
  <div class="ai" :class="item.level" @click="$emit('click',item)">
    <div class="ai-icon"><div class="ai-dot"></div></div>
    <div class="ai-body"><div class="ai-title">{{item.title}}</div><div class="ai-desc">{{item.description}}</div><div class="ai-dist">{{(item.distanceM/1000).toFixed(1)}}km</div></div>
  </div>
</template>
<script setup>
defineProps({item:Object}); defineEmits(['click'])
</script>
<style scoped>
.ai{display:flex;gap:10px;padding:10px 12px;border-radius:8px;margin-bottom:8px;cursor:pointer;border-left:3px solid}
.ai.critical{border-color:#E53935;background:#ffebee}.ai.warning{border-color:#FF9800;background:#fff3e0}.ai.info{border-color:#1976D2;background:#e3f2fd}
.ai-dot{width:10px;height:10px;border-radius:50%}.critical .ai-dot{background:#E53935}.warning .ai-dot{background:#FF9800}.info .ai-dot{background:#1976D2}
.ai-body{flex:1}.ai-title{font-size:14px;font-weight:600;color:#333}.ai-desc{font-size:12px;color:#666;margin-top:2px}.ai-dist{font-size:11px;color:#999;margin-top:4px}
</style>
