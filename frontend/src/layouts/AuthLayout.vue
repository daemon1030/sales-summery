<script setup lang="ts">
import { RouterView } from "vue-router";
</script>

<template>
  <main class="auth-shell">
    <section class="auth-brand">
      <span class="brand-mark">S</span>
      <p class="eyebrow">SALES SUMMARY</p>
      <h1>매일의 기록이<br>사업의 방향이 됩니다.</h1>
      <p>수입과 지출, 날짜별 메모를 한곳에서 관리하고 현재 손익을 빠르게 확인하세요.</p>
    </section>
    <section class="auth-panel">
      <RouterView />
    </section>
  </main>
</template>
