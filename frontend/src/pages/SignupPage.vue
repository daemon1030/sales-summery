<script setup lang="ts">
import { ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { authApi } from "../api";

const router = useRouter();
const form = ref({ loginId: "", password: "", name: "" });
const loading = ref(false);
const error = ref("");

async function submit() {
  loading.value = true;
  error.value = "";
  try {
    await authApi.signup(form.value);
    await router.push({ name: "login", query: { joined: "true" } });
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "회원가입하지 못했습니다.";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="auth-card">
    <div class="auth-card-heading"><p class="eyebrow">START TODAY</p><h2>회원가입</h2><p>기본 수입·지출 항목 5개가 자동으로 준비됩니다.</p></div>
    <form class="form-stack" @submit.prevent="submit">
      <label>이름<input v-model.trim="form.name" maxlength="50" required placeholder="이름을 입력하세요"></label>
      <label>로그인 아이디<input v-model.trim="form.loginId" pattern="[A-Za-z0-9_]{4,10}" autocomplete="username" required placeholder="영문·숫자·밑줄 4~10자"></label>
      <label>비밀번호<input v-model="form.password" type="password" minlength="8" maxlength="72" autocomplete="new-password" required placeholder="8자 이상 입력하세요"></label>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <button class="button primary wide" :disabled="loading">{{ loading ? '가입 중…' : '계정 만들기' }}</button>
    </form>
    <p class="auth-switch">이미 계정이 있나요? <RouterLink to="/login">로그인</RouterLink></p>
  </div>
</template>
