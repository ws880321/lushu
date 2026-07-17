<template>
  <div>
    <h2>{{ isNew ? 'Create POI' : 'Edit POI' }}</h2>
    <p>POI editor will appear here.</p>
    <el-button @click="$router.push('/pois')">Back</el-button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
const route = useRoute()
const isNew = computed(() => route.name === 'PoiCreate')
</script>
