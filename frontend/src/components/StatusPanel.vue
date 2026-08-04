<script setup lang="ts">
defineProps<{ loading?: boolean; error?: string; empty?: boolean; emptyText?: string }>();
</script>

<template>
  <div v-if="loading" class="status-panel"><span class="spinner"></span><p>데이터를 불러오고 있습니다.</p></div>
  <div v-else-if="error" class="status-panel error"><strong>확인해 주세요</strong><p>{{ error }}</p></div>
  <div v-else-if="empty" class="status-panel"><strong>아직 데이터가 없습니다</strong><p>{{ emptyText ?? '첫 기록을 추가해 보세요.' }}</p></div>
</template>
