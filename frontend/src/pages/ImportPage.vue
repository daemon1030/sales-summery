<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { categoryApi, importApi } from "../api";
import type {
  Category, ImportAnalysis, ImportColumnMapping, ImportResult,
} from "../api/types";
import { currency } from "../utils/format";

const files = ref<File[]>([]);
const categories = ref<Category[]>([]);
const analysis = ref<ImportAnalysis | null>(null);
const mappings = ref<ImportColumnMapping[]>([]);
const result = ref<ImportResult | null>(null);
const loading = ref(false);
const error = ref("");
const downloading = ref(false);

const canConfirm = computed(() => mappings.value.every(mapping => {
  if (mapping.action === "IGNORE") return true;
  if (mapping.action === "EXISTING") return Boolean(mapping.categoryId);
  return Boolean(mapping.categoryName?.trim() && mapping.transactionType && mapping.costType && mapping.frequency);
}));

function selectFiles(event: Event) {
  const input = event.target as HTMLInputElement;
  files.value = Array.from(input.files ?? []);
  analysis.value = null;
  mappings.value = [];
  result.value = null;
  error.value = "";
}

async function downloadTemplate() {
  downloading.value = true;
  error.value = "";
  try {
    const { blob, filename } = await importApi.template();
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "기본 양식을 내려받지 못했습니다.";
  } finally {
    downloading.value = false;
  }
}

async function analyze() {
  if (!files.value.length) return;
  loading.value = true;
  error.value = "";
  result.value = null;
  try {
    analysis.value = await importApi.analyze(files.value);
    mappings.value = analysis.value.columns
      .filter(column => column.status === "NEEDS_MAPPING")
      .map(column => ({
        header: column.header,
        action: "CREATE",
        categoryId: null,
        categoryName: column.header,
        transactionType: "EXPENSE",
        costType: "VARIABLE",
        frequency: "IRREGULAR",
      }));
    if (analysis.value.readyFileCount > 0 && !analysis.value.requiresMapping) await confirm();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "엑셀 파일을 분석하지 못했습니다.";
  } finally {
    loading.value = false;
  }
}

function changeAction(mapping: ImportColumnMapping) {
  if (mapping.action === "EXISTING") {
    mapping.categoryId = categories.value[0]?.categoryId ?? null;
  } else if (mapping.action === "CREATE") {
    mapping.categoryId = null;
    mapping.categoryName ||= mapping.header;
    mapping.transactionType ||= "EXPENSE";
    mapping.costType = mapping.transactionType === "INCOME" ? "NONE" : mapping.costType === "NONE" ? "VARIABLE" : mapping.costType;
    mapping.frequency ||= "IRREGULAR";
  }
}

function changeTransactionType(mapping: ImportColumnMapping) {
  mapping.costType = mapping.transactionType === "INCOME" ? "NONE" : "VARIABLE";
}

async function confirm() {
  if (!analysis.value || !canConfirm.value || analysis.value.readyFileCount === 0) return;
  loading.value = true;
  error.value = "";
  try {
    result.value = await importApi.confirm(analysis.value.importToken, mappings.value);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "엑셀 데이터를 등록하지 못했습니다.";
  } finally {
    loading.value = false;
  }
}

function reset() {
  files.value = [];
  analysis.value = null;
  mappings.value = [];
  result.value = null;
  error.value = "";
}

function statusLabel(status: string) {
  if (status === "READY") return "등록 가능";
  if (status === "DUPLICATE") return "중복 제외";
  return "오류";
}

onMounted(async () => {
  try { categories.value = await categoryApi.list(true); }
  catch (cause) { error.value = cause instanceof Error ? cause.message : "카테고리를 불러오지 못했습니다."; }
});
</script>

<template>
  <section class="page-heading">
    <div>
      <p class="eyebrow">EXCEL IMPORT</p>
      <h2>엑셀 가져오기</h2>
      <p>하루 한 줄로 작성한 엑셀 여러 개를 분석해 수입·지출 기록으로 자동 등록합니다.</p>
    </div>
    <button class="button secondary" :disabled="downloading" @click="downloadTemplate">
      {{ downloading ? "양식 만드는 중" : "기본 양식 다운로드" }}
    </button>
  </section>

  <p v-if="error" class="toast error">{{ error }}</p>

  <section v-if="!analysis && !result" class="panel-card import-upload-card">
    <div class="import-upload-copy">
      <span class="import-icon">XLSX</span>
      <div>
        <h3>가계부 엑셀을 선택하세요</h3>
        <p>한 번에 최대 50개, 파일당 20MB까지 선택할 수 있습니다. 숫자와 숫자 결과를 반환하는 수식을 가져옵니다.</p>
      </div>
    </div>
    <label class="file-picker">
      <input type="file" accept=".xlsx,.xls" multiple @change="selectFiles">
      <span>{{ files.length ? `${files.length}개 파일 선택됨` : "엑셀 파일 선택" }}</span>
    </label>
    <ul v-if="files.length" class="selected-files">
      <li v-for="file in files" :key="`${file.name}-${file.size}`">
        <span>{{ file.name }}</span><small>{{ Math.ceil(file.size / 1024).toLocaleString() }}KB</small>
      </li>
    </ul>
    <button class="button primary wide" :disabled="!files.length || loading" @click="analyze">
      {{ loading ? "분석 중" : "분석하고 자동 등록" }}
    </button>
  </section>

  <template v-if="analysis && !result">
    <section class="import-summary-grid">
      <article><span>등록 가능</span><strong>{{ analysis.readyFileCount }}</strong><small>개 파일</small></article>
      <article><span>중복 제외</span><strong>{{ analysis.duplicateFileCount }}</strong><small>개 파일</small></article>
      <article><span>오류</span><strong>{{ analysis.invalidFileCount }}</strong><small>개 파일</small></article>
    </section>

    <section class="panel-card table-card import-files-card">
      <div class="card-heading"><h3>파일 분석 결과</h3></div>
      <div class="table-scroll">
        <table>
          <thead><tr><th>파일</th><th>상태</th><th class="number">날짜 행</th><th class="number">거래 예정</th><th>내용</th></tr></thead>
          <tbody>
            <tr v-for="file in analysis.files" :key="file.fileName">
              <td>{{ file.fileName }}</td>
              <td><span class="badge" :class="file.status === 'READY' ? 'active' : file.status === 'DUPLICATE' ? 'muted' : 'expense'">{{ statusLabel(file.status) }}</span></td>
              <td class="number">{{ file.rowCount }}</td>
              <td class="number">{{ file.recordCount }}</td>
              <td class="import-error-cell">{{ file.errors.join(" · ") || "정상" }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-if="mappings.length" class="panel-card import-mapping-card">
      <div class="card-heading">
        <div><h3>새로운 열 설정</h3><p>처음 발견한 열만 설정하면 다음 파일부터는 같은 이름의 카테고리에 자동 연결됩니다.</p></div>
      </div>
      <article v-for="mapping in mappings" :key="mapping.header" class="mapping-row">
        <div class="mapping-title">
          <strong>{{ mapping.header }}</strong>
          <small>{{ analysis.columns.find(column => column.header === mapping.header)?.valueCount }}건 · {{ currency(analysis.columns.find(column => column.header === mapping.header)?.totalAmount ?? 0) }}</small>
        </div>
        <label>처리 방법
          <select v-model="mapping.action" @change="changeAction(mapping)">
            <option value="CREATE">새 카테고리 생성</option>
            <option value="EXISTING">기존 카테고리에 연결</option>
            <option value="IGNORE">가져오지 않기</option>
          </select>
        </label>
        <label v-if="mapping.action === 'EXISTING'">기존 카테고리
          <select v-model.number="mapping.categoryId" required>
            <option v-for="category in categories" :key="category.categoryId" :value="category.categoryId">{{ category.categoryName }}</option>
          </select>
        </label>
        <template v-if="mapping.action === 'CREATE'">
          <label>카테고리명<input v-model.trim="mapping.categoryName" maxlength="100" required></label>
          <label>수입·지출
            <select v-model="mapping.transactionType" @change="changeTransactionType(mapping)">
              <option value="INCOME">수입</option><option value="EXPENSE">지출</option>
            </select>
          </label>
          <label>비용유형
            <select v-model="mapping.costType" :disabled="mapping.transactionType === 'INCOME'">
              <option v-if="mapping.transactionType === 'INCOME'" value="NONE">해당 없음</option>
              <template v-else><option value="VARIABLE">변동비</option><option value="FIXED">고정비</option></template>
            </select>
          </label>
          <label>빈도
            <select v-model="mapping.frequency"><option value="DAILY">매일</option><option value="MONTHLY">매월</option><option value="IRREGULAR">비정기</option></select>
          </label>
        </template>
      </article>
    </section>

    <div class="import-actions">
      <button class="button secondary" :disabled="loading" @click="reset">다른 파일 선택</button>
      <button v-if="mappings.length && analysis.readyFileCount" class="button primary" :disabled="loading || !canConfirm" @click="confirm">
        {{ loading ? "등록 중" : "설정 적용하고 등록" }}
      </button>
      <p v-else-if="!analysis.readyFileCount">등록 가능한 파일이 없습니다.</p>
      <p v-else-if="loading">기존 카테고리로 자동 등록하고 있습니다.</p>
    </div>
  </template>

  <section v-if="result" class="panel-card import-result-card">
    <span class="result-check">✓</span>
    <h3>엑셀 가져오기가 완료되었습니다</h3>
    <div class="result-numbers">
      <div><strong>{{ result.importedFileCount }}</strong><span>등록 파일</span></div>
      <div><strong>{{ result.createdRecordCount }}</strong><span>거래 기록</span></div>
      <div><strong>{{ result.createdCategoryCount }}</strong><span>새 카테고리</span></div>
      <div><strong>{{ result.createdNoteCount }}</strong><span>특이사항</span></div>
    </div>
    <p v-if="result.duplicateFileCount || result.skippedNoteCount">중복 파일 {{ result.duplicateFileCount }}개와 이미 존재하는 특이사항 {{ result.skippedNoteCount }}건은 제외했습니다.</p>
    <button class="button primary" @click="reset">다른 엑셀 가져오기</button>
  </section>
</template>
