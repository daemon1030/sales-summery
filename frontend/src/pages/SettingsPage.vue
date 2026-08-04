<script setup lang="ts">
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import { userApi } from "../api";
import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();
const router = useRouter();
const name = ref(auth.user?.name ?? "");
const settlementStartDay = ref(auth.user?.settlementStartDay ?? 1);
const currentPassword = ref("");
const newPassword = ref("");
const withdrawPassword = ref("");
const message = ref("");
const error = ref("");
const saving = ref("");
const initials = computed(() => auth.user?.name?.slice(0, 1) ?? "S");

async function run(key: string, action: () => Promise<void>, success: string) {
  saving.value = key;
  message.value = "";
  error.value = "";
  try { await action(); message.value = success; }
  catch (cause) { error.value = cause instanceof Error ? cause.message : "설정을 변경하지 못했습니다."; }
  finally { saving.value = ""; }
}

function saveName() {
  return run("name", async () => auth.updateUser(await userApi.changeName(name.value.trim())), "이름을 변경했습니다.");
}

function saveSettlement() {
  return run("settlement", async () => {
    await userApi.changeSettlementDay(settlementStartDay.value);
    if (auth.user) auth.updateUser({ ...auth.user, settlementStartDay: settlementStartDay.value });
  }, "정산 시작일을 변경했습니다.");
}

function savePassword() {
  return run("password", async () => {
    await userApi.changePassword(currentPassword.value, newPassword.value);
    currentPassword.value = "";
    newPassword.value = "";
  }, "비밀번호를 변경했습니다.");
}

async function withdraw() {
  if (!window.confirm("정말 회원 탈퇴를 진행할까요? 이 작업은 즉시 계정을 비활성화합니다.")) return;
  await run("withdraw", async () => {
    await userApi.withdraw(withdrawPassword.value);
    auth.clearSession();
    await router.push({ name: "login" });
  }, "");
}
</script>

<template>
  <section class="page-heading">
    <div><p class="eyebrow">SETTINGS</p><h2>내 정보와 정산 설정</h2><p>계정 정보와 사업장의 정산 기준을 관리합니다.</p></div>
  </section>

  <div v-if="message" class="toast success" role="status">{{ message }}</div>
  <div v-if="error" class="toast error" role="alert">{{ error }}</div>

  <section class="settings-grid">
    <article class="panel-card profile-card">
      <div class="profile-summary"><span class="avatar large">{{ initials }}</span><div><h3>{{ auth.user?.name }}</h3><p>{{ auth.user?.loginId }}</p><span class="badge active">활성 계정</span></div></div>
      <form class="form-stack" @submit.prevent="saveName">
        <label>이름<input v-model.trim="name" maxlength="50" required></label>
        <button class="button secondary" :disabled="saving === 'name'">이름 변경</button>
      </form>
    </article>

    <article class="panel-card">
      <header class="card-heading"><div><p class="eyebrow">SETTLEMENT</p><h3>정산 기준</h3></div></header>
      <p class="section-description">매월 선택한 날짜부터 다음 달 전날까지를 하나의 정산 기간으로 계산합니다.</p>
      <form class="form-stack" @submit.prevent="saveSettlement">
        <label>정산 시작일<input v-model.number="settlementStartDay" type="number" min="1" max="28" required><small>1일부터 28일 사이에서 선택하세요.</small></label>
        <button class="button secondary" :disabled="saving === 'settlement'">정산 시작일 저장</button>
      </form>
    </article>

    <article class="panel-card">
      <header class="card-heading"><div><p class="eyebrow">SECURITY</p><h3>비밀번호 변경</h3></div></header>
      <form class="form-stack" @submit.prevent="savePassword">
        <label>현재 비밀번호<input v-model="currentPassword" type="password" autocomplete="current-password" required></label>
        <label>새 비밀번호<input v-model="newPassword" type="password" minlength="8" maxlength="72" autocomplete="new-password" required><small>8자 이상 입력하세요.</small></label>
        <button class="button secondary" :disabled="saving === 'password'">비밀번호 변경</button>
      </form>
    </article>

    <article class="panel-card danger-zone">
      <header class="card-heading"><div><p class="eyebrow">DANGER ZONE</p><h3>회원 탈퇴</h3></div></header>
      <p class="section-description">탈퇴 즉시 로그인이 제한됩니다. 현재 비밀번호를 입력해야 진행할 수 있습니다.</p>
      <form class="form-stack" @submit.prevent="withdraw">
        <label>현재 비밀번호<input v-model="withdrawPassword" type="password" required></label>
        <button class="button danger" :disabled="saving === 'withdraw'">회원 탈퇴</button>
      </form>
    </article>
  </section>
</template>
