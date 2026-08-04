<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { categoryApi, recordApi, type RecordPayload } from "../api";
import type { Category, FinancialRecord, PageResponse, TransactionType } from "../api/types";
import BaseModal from "../components/BaseModal.vue";
import StatusPanel from "../components/StatusPanel.vue";
import { currency, monthRange, shortDate, todayInKorea } from "../utils/format";

const range = monthRange();
const filters = reactive({ startDate: range.startDate, endDate: range.endDate, categoryId: "", transactionType: "" });
const page = ref<PageResponse<FinancialRecord> | null>(null);
const categories = ref<Category[]>([]);
const loading = ref(true);
const error = ref("");
const modalOpen = ref(false);
const saving = ref(false);
const editingId = ref<number | null>(null);
const formError = ref("");
const form = reactive<RecordPayload>({ categoryId: 0, recordDate: todayInKorea(), amount: 0, memo: "" });

async function load(pageNumber = 0) {
  loading.value = true;
  error.value = "";
  try {
    page.value = await recordApi.search({
      startDate: filters.startDate,
      endDate: filters.endDate,
      categoryId: filters.categoryId ? Number(filters.categoryId) : undefined,
      transactionType: (filters.transactionType || undefined) as TransactionType | undefined,
      page: pageNumber,
      size: 20,
    });
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "기록을 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editingId.value = null;
  Object.assign(form, { categoryId: categories.value[0]?.categoryId ?? 0, recordDate: todayInKorea(), amount: 0, memo: "" });
  formError.value = "";
  modalOpen.value = true;
}

function openEdit(record: FinancialRecord) {
  editingId.value = record.recordId;
  Object.assign(form, { categoryId: record.categoryId, recordDate: record.recordDate, amount: Number(record.amount), memo: record.memo ?? "" });
  formError.value = "";
  modalOpen.value = true;
}

async function save() {
  saving.value = true;
  formError.value = "";
  try {
    const payload = { ...form, memo: form.memo?.trim() || null };
    if (editingId.value) await recordApi.update(editingId.value, payload);
    else await recordApi.create(payload);
    modalOpen.value = false;
    await load(page.value?.page ?? 0);
  } catch (cause) {
    formError.value = cause instanceof Error ? cause.message : "기록을 저장하지 못했습니다.";
  } finally {
    saving.value = false;
  }
}

async function remove(record: FinancialRecord) {
  if (!window.confirm(`${record.categoryName} 기록을 삭제할까요?`)) return;
  try {
    await recordApi.remove(record.recordId);
    await load(page.value?.page ?? 0);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "기록을 삭제하지 못했습니다.";
  }
}

onMounted(async () => {
  try { categories.value = await categoryApi.list(true); }
  catch (cause) { error.value = cause instanceof Error ? cause.message : "항목을 불러오지 못했습니다."; }
  await load();
});
</script>

<template>
  <section class="page-heading">
    <div><p class="eyebrow">TRANSACTIONS</p><h2>수입과 지출 기록</h2><p>기간과 항목으로 찾고 잘못 입력한 기록은 바로 수정할 수 있습니다.</p></div>
    <button class="button primary" :disabled="categories.length === 0" @click="openCreate">+ 기록 등록</button>
  </section>

  <form class="filter-bar" @submit.prevent="load(0)">
    <label>시작일<input v-model="filters.startDate" type="date" required></label>
    <label>종료일<input v-model="filters.endDate" type="date" required></label>
    <label>항목<select v-model="filters.categoryId"><option value="">전체 항목</option><option v-for="item in categories" :key="item.categoryId" :value="String(item.categoryId)">{{ item.categoryName }}</option></select></label>
    <label>구분<select v-model="filters.transactionType"><option value="">전체</option><option value="INCOME">수입</option><option value="EXPENSE">지출</option></select></label>
    <button class="button secondary">조회</button>
  </form>

  <section class="panel-card table-card">
    <header class="card-heading"><div><p class="eyebrow">RECORD LIST</p><h3>기록 {{ page?.totalElements ?? 0 }}건</h3></div></header>
    <StatusPanel :loading="loading" :error="error" :empty="!loading && !error && page?.content.length === 0" />
    <div v-if="page?.content.length" class="table-scroll">
      <table>
        <thead><tr><th>날짜</th><th>항목</th><th>구분</th><th class="number">금액</th><th>메모</th><th></th></tr></thead>
        <tbody>
          <tr v-for="record in page.content" :key="record.recordId">
            <td>{{ shortDate(record.recordDate) }}</td>
            <td><strong>{{ record.categoryName }}</strong></td>
            <td><span class="badge" :class="record.transactionType.toLowerCase()">{{ record.transactionType === 'INCOME' ? '수입' : '지출' }}</span></td>
            <td class="number amount" :class="record.transactionType.toLowerCase()">{{ currency(record.amount) }}</td>
            <td class="memo-cell">{{ record.memo || '—' }}</td>
            <td class="actions"><button class="text-button" @click="openEdit(record)">수정</button><button class="text-button danger" @click="remove(record)">삭제</button></td>
          </tr>
        </tbody>
      </table>
    </div>
    <footer v-if="page && page.totalPages > 1" class="pagination">
      <button class="button secondary compact" :disabled="page.first" @click="load(page.page - 1)">이전</button>
      <span>{{ page.page + 1 }} / {{ page.totalPages }}</span>
      <button class="button secondary compact" :disabled="page.last" @click="load(page.page + 1)">다음</button>
    </footer>
  </section>

  <BaseModal v-if="modalOpen" :title="editingId ? '기록 수정' : '새 기록 등록'" @close="modalOpen = false">
    <form class="form-stack" @submit.prevent="save">
      <label>항목<select v-model.number="form.categoryId" required><option v-for="item in categories" :key="item.categoryId" :value="item.categoryId">{{ item.categoryName }} · {{ item.transactionType === 'INCOME' ? '수입' : '지출' }}</option></select></label>
      <div class="form-row"><label>날짜<input v-model="form.recordDate" type="date" required></label><label>금액<input v-model.number="form.amount" type="number" min="0.01" step="0.01" required></label></div>
      <label>메모<textarea v-model="form.memo" maxlength="500" rows="4" placeholder="선택 입력"></textarea></label>
      <p v-if="formError" class="form-error">{{ formError }}</p>
      <div class="form-actions"><button type="button" class="button secondary" @click="modalOpen = false">취소</button><button class="button primary" :disabled="saving">{{ saving ? '저장 중…' : '저장' }}</button></div>
    </form>
  </BaseModal>
</template>
