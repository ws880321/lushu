<template>
  <div v-if="error" class="eb">
    <van-empty image="error" description="页面加载出错">
      <van-button round type="primary" size="small" @click="error=null">重试</van-button>
    </van-empty>
  </div>
  <slot v-else />
</template>
<script setup>
import { ref, onErrorCaptured } from 'vue'
const error = ref(null)
onErrorCaptured((err) => { error.value = err.message || '未知错误'; return false })
</script>
<style scoped>.eb{padding:40px 20px}</style>
