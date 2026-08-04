<script setup lang="ts">
import { onMounted, reactive, ref, watch } from "vue";
import { categoryApi } from "../api";
import type { Category, CostType, Frequency, TransactionType } from "../api/types";
import BaseModal from "../components/BaseModal.vue";
import StatusPanel from "../components/StatusPanel.vue";

const categories = ref<Category[]>([]);
const loading = ref(true);
const error = ref("");
const showInactive = ref(true);
const modalOpen = ref(false);
const saving = ref(false);
const formError = ref("");
const form = reactive<{ categoryName: string; transactionType: TransactionType; costType: CostType; frequency: Frequency }>({
  categoryName: "", transactionType: "INCOME", costType: "NONE", frequency: "DAILY",
});

watch(() => form.transactionType, type => {
  form.costType = type === "INCOME" ? "NONE" : "VARIABLE";
});

async function load() {
  loading.value = true;
  error.value = "";
  try { categories.value = await categoryApi.list(!showInactive.value); }
  catch (cause) { error.value = cause instanceof Error ? cause.message : "항목을 불러오지 못했습니다."; }
  finally { loading.value = false; }
}

function openCreate() {
  Object.assign(form, { categoryName: "", transactionType: "INCOME", costType: "NONE", frequency: "DAILY" });
  formError.value = "";
  modalOpen.value = true;
}

async function create() {
  saving.value = true;
  formError.value = "";
  try {
    await categoryApi.create(form);
    modalOpen.value = false;
    await load();
  } catch (cause) {
    formError.value = cause instanceof Error ? cause.message : "항목을 만들지 못했습니다.";
  } finally { saving.value = false; }
}

async function rename(item: Category) {
  const name = window.prompt("새 항목명을 입력하세요.", item.categoryName)?.trim();
  if (!name || name === item.categoryName) return;
  try { await categoryApi.rename(item.categoryId, name); await load(); }
  catch (cause) { error.value = cause instanceof Error ? cause.message : "이름을 변경하지 못했습니다."; }
}

async function toggle(item: Category) {
  try {
    if (item.active) await categoryApi.deactivate(item.categoryId);
    else await categoryApi.activate(item.categoryId);
    await load();
  } catch (cause) { error.value = cause instanceof Error ? cause.message : "상태를 변경하지 못했습니다."; }
}

onMounted(load);
</script>

<template>
  <section class="page-heading">
    <div><p class="eyebrow">CATEGORIES</p><h2>수입·지출 항목</h2><p>기록을 분류할 항목을 관리합니다. 사용하던 항목은 삭제하지 않고 비활성화합니다.</p></div>
    <button class="button primary" @click="openCreate">+ 항목 추가</button>
  </section>

  <div class="section-toolbar">
    <label class="check-label"><input v-model="showInactive" type="checkbox" @change="load"> 비활성 항목 포함</label>
    <span>총 {{ categories.length }}개</span>
  </div>

  <StatusPanel :loading="loading" :error="error" :empty="!loading && !error && categories.length === 0" />
  <section v-if="categories.length" class="category-grid">
    <article v-for="item in categories" :key="item.categoryId" class="category-card" :class="{ inactive: !item.active }">
      <header><span class="category-symbol" :class="item.transactionType.toLowerCase()">{{ item.transactionType === 'INCOME' ? '↗' : '↘' }}</span><span class="badge" :class="item.active ? 'active' : 'muted'">{{ item.active ? '사용 중' : '비활성' }}</span></header>
      <h3>{{ item.categoryName }}</h3>
      <p>{{ item.transactionType === 'INCOME' ? '수입' : '지출' }} · {{ item.costType === 'NONE' ? '비용 구분 없음' : item.costType === 'FIXED' ? '고정비' : '변동비' }} · {{ item.frequency === 'DAILY' ? '매일' : item.frequency === 'MONTHLY' ? '매월' : '비정기' }}</p>
      <footer><button class="text-button" @click="rename(item)">이름 변경</button><button class="text-button" :class="item.active ? 'danger' : ''" @click="toggle(item)">{{ item.active ? '비활성화' : '활성화' }}</button></footer>
    </article>
  </section>

  <BaseModal v-if="modalOpen" title="새 항목 추가" @close="modalOpen = false">
    <form class="form-stack" @submit.prevent="create">
      <label>항목명<input v-model.trim="form.categoryName" maxlength="100" required placeholder="예: 배달 매출"></label>
      <div class="form-row"><label>거래 유형<select v-model="form.transactionType"><option value="INCOME">수입</option><option value="EXPENSE">지출</option></select></label><label>발생 주기<select v-model="form.frequency"><option value="DAILY">매일</option><option value="MONTHLY">매월</option><option value="IRREGULAR">비정기</option></select></label></div>
      <label>비용 유형<select v-model="form.costType" :disabled="form.transactionType === 'INCOME'"><option v-if="form.transactionType === 'INCOME'" value="NONE">해당 없음</option><template v-else><option value="FIXED">고정비</option><option value="VARIABLE">변동비</option></template></select></label>
      <p v-if="formError" class="form-error">{{ formError }}</p>
      <div class="form-actions"><button type="button" class="button secondary" @click="modalOpen = false">취소</button><button class="button primary" :disabled="saving">{{ saving ? '추가 중…' : '추가' }}</button></div>
    </form>
  </BaseModal>
</template>
