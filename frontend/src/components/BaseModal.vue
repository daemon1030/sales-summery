<script setup lang="ts">
defineProps<{ title: string }>();
defineEmits<{ close: [] }>();
</script>

<template>
  <div class="modal-backdrop" role="presentation" @mousedown.self="$emit('close')">
    <section class="modal" role="dialog" aria-modal="true" :aria-label="title">
      <header><div><p class="eyebrow">SALES SUMMARY</p><h2>{{ title }}</h2></div><button class="icon-button" aria-label="닫기" @click="$emit('close')">×</button></header>
      <slot />
    </section>
  </div>
</template>
