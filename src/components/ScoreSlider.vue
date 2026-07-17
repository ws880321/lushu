<template>
  <div class="score-slider">
    <el-slider
      :model-value="modelValue"
      :min="0.5"
      :max="5.0"
      :step="0.5"
      show-stops
      style="width: 180px; margin-right: 12px"
      @update:model-value="$emit('update:modelValue', $event)"
    />
    <span class="score-value">{{ modelValue }}</span>
  </div>
</template>

<script setup>
defineProps({
  modelValue: {
    type: Number,
    default: 3.0,
  },
})

defineEmits(['update:modelValue'])
</script>

<style scoped>
.score-slider {
  display: flex;
  align-items: center;
}
.score-value {
  font-weight: 600;
  min-width: 2em;
  color: var(--el-color-primary);
}
</style>
