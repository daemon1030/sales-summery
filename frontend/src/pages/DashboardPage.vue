<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { dashboardApi } from "../api";
import type { CategoryTotal, FinancialRecord, PeriodSummary, ProfitSummary } from "../api/types";
import StatusPanel from "../components/StatusPanel.vue";
import { currency, shortDate } from "../utils/format";

const loading = ref(true);
const error = ref("");
const today = ref<ProfitSummary>({ totalIncome: 0, totalExpense: 0, netProfit: 0 });
const settlement = ref<PeriodSummary | null>(null);
const recent = ref<FinancialRecord[]>([]);
const totals = ref<CategoryTotal[]>([]);
const maxTotal = computed(() => Math.max(1, ...totals.value.map(item => Number(item.totalAmount))));

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const [todayData, period, recentData] = await Promise.all([
      dashboardApi.today(), dashboardApi.current(), dashboardApi.recent(),
    ]);
    today.value = todayData;
    settlement.value = period;
    recent.value = recentData;
    totals.value = await dashboardApi.categoryTotals(period.startDate, period.endDate);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "대시보드를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="page-heading">
    <div><p class="eyebrow">OVERVIEW</p><h2>사업장의 흐름을 한눈에 확인하세요.</h2><p v-if="settlement">현재 정산 기간 {{ settlement.startDate }} — {{ settlement.endDate }}</p></div>
    <RouterLink class="button primary" to="/records">+ 새 기록</RouterLink>
  </section>

  <StatusPanel :loading="loading" :error="error" />
  <template v-if="!loading && !error">
    <section class="summary-grid">
      <article class="summary-card income"><span>오늘 총수입</span><strong>{{ currency(today.totalIncome) }}</strong><small>오늘 입력된 수입 합계</small></article>
      <article class="summary-card expense"><span>오늘 총지출</span><strong>{{ currency(today.totalExpense) }}</strong><small>오늘 입력된 지출 합계</small></article>
      <article class="summary-card profit"><span>정산 기간 순이익</span><strong>{{ currency(settlement?.summary.netProfit ?? 0) }}</strong><small>수입에서 지출을 제외한 금액</small></article>
    </section>

    <section class="dashboard-grid">
      <article class="panel-card chart-card">
        <header class="card-heading"><div><p class="eyebrow">CATEGORY TOTALS</p><h3>정산 기간 항목별 합계</h3></div><RouterLink to="/categories">항목 관리</RouterLink></header>
        <StatusPanel :empty="totals.length === 0" empty-text="기록을 추가하면 항목별 합계가 표시됩니다." />
        <div v-if="totals.length" class="bar-list">
          <div v-for="item in totals" :key="item.categoryId" class="bar-row">
            <div class="bar-label"><span>{{ item.categoryName }}</span><strong>{{ currency(item.totalAmount) }}</strong></div>
            <div class="bar-track"><span :class="item.transactionType.toLowerCase()" :style="{ width: `${Math.max(4, Number(item.totalAmount) / maxTotal * 100)}%` }"></span></div>
          </div>
        </div>
      </article>

      <article class="panel-card recent-card">
        <header class="card-heading"><div><p class="eyebrow">RECENT</p><h3>최근 기록</h3></div><RouterLink to="/records">전체 보기</RouterLink></header>
        <StatusPanel :empty="recent.length === 0" />
        <ul v-if="recent.length" class="record-list compact-list">
          <li v-for="record in recent" :key="record.recordId">
            <span class="record-icon" :class="record.transactionType.toLowerCase()">{{ record.transactionType === 'INCOME' ? '↗' : '↘' }}</span>
            <div><strong>{{ record.categoryName }}</strong><small>{{ shortDate(record.recordDate) }}<template v-if="record.memo"> · {{ record.memo }}</template></small></div>
            <b :class="record.transactionType.toLowerCase()">{{ record.transactionType === 'INCOME' ? '+' : '-' }}{{ currency(record.amount) }}</b>
          </li>
        </ul>
      </article>
    </section>
  </template>
</template>
