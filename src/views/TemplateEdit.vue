<template>
  <div>
    <h2>{{ isNew ? 'Create Template' : 'Edit Template' }}</h2>
    <p>Template editor will appear here.</p>
    <el-button @click="$router.push('/templates')">Back</el-button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
const route = useRoute()
const isNew = computed(() => route.name === 'TemplateCreate')
</script>
