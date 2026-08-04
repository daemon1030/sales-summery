<script setup lang="ts">
import { ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const loginId = ref("");
const password = ref("");
const loading = ref(false);
const error = ref("");

async function submit() {
  loading.value = true;
  error.value = "";
  try {
    await auth.login(loginId.value, password.value);
    const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "/dashboard";
    await router.push(redirect);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "로그인하지 못했습니다.";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="auth-card">
    <div class="auth-card-heading"><p class="eyebrow">WELCOME BACK</p><h2>로그인</h2><p>사업장의 오늘을 확인해 보세요.</p></div>
    <form class="form-stack" @submit.prevent="submit">
      <label>로그인 아이디<input v-model.trim="loginId" autocomplete="username" minlength="4" maxlength="10" required placeholder="아이디를 입력하세요"></label>
      <label>비밀번호<input v-model="password" type="password" autocomplete="current-password" minlength="8" required placeholder="비밀번호를 입력하세요"></label>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <button class="button primary wide" :disabled="loading">{{ loading ? '로그인 중…' : '로그인' }}</button>
    </form>
    <p class="auth-switch">아직 계정이 없나요? <RouterLink to="/signup">회원가입</RouterLink></p>
  </div>
</template>
