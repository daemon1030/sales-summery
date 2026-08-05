<script setup lang="ts">
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { dashboardApi } from "../api";
import type {
  CategoryBreakdown, FinancialRecord, PeriodSummary, ProfitMetric,
  ProfitSummary, ProfitTrendPoint, TrendUnit,
} from "../api/types";
import CategoryBreakdownChart from "../components/CategoryBreakdownChart.vue";
import ProfitTrendChart from "../components/ProfitTrendChart.vue";
import StatusPanel from "../components/StatusPanel.vue";
import { currency, shortDate, todayInKorea } from "../utils/format";

const loading = ref(true);
const error = ref("");
const today = ref<ProfitSummary>({ totalIncome: 0, totalExpense: 0, netProfit: 0 });
const settlement = ref<PeriodSummary | null>(null);
const recent = ref<FinancialRecord[]>([]);

const trendLoading = ref(false);
const trendError = ref("");
const trend = ref<ProfitTrendPoint[]>([]);
const trendUnit = ref<TrendUnit>("DAILY");
const profitMetric = ref<ProfitMetric>("INCOME");
const currentYear = Number(todayInKorea().slice(0, 4));
const dailyStart = ref("");
const dailyEnd = ref("");
const monthlyYear = ref(currentYear);
const yearlyStart = ref(currentYear - 4);
const yearlyEnd = ref(currentYear);

const breakdownLoading = ref(false);
const breakdownError = ref("");
const breakdownMonth = ref(todayInKorea().slice(0, 7));
const expenseBreakdown = ref<CategoryBreakdown | null>(null);
const incomeBreakdown = ref<CategoryBreakdown | null>(null);

async function loadTrend() {
  trendLoading.value = true;
  trendError.value = "";
  try {
    const params = trendUnit.value === "DAILY"
      ? { unit: trendUnit.value, startDate: dailyStart.value, endDate: dailyEnd.value }
      : trendUnit.value === "MONTHLY"
        ? { unit: trendUnit.value, year: monthlyYear.value }
        : { unit: trendUnit.value, startYear: yearlyStart.value, endYear: yearlyEnd.value };
    trend.value = await dashboardApi.profitTrend(params);
  } catch (cause) {
    trendError.value = cause instanceof Error ? cause.message : "손익 추이를 불러오지 못했습니다.";
  } finally {
    trendLoading.value = false;
  }
}

async function changeUnit(unit: TrendUnit) {
  trendUnit.value = unit;
  await loadTrend();
}

async function loadBreakdowns() {
  breakdownLoading.value = true;
  breakdownError.value = "";
  const [year, month] = breakdownMonth.value.split("-").map(Number);
  try {
    [expenseBreakdown.value, incomeBreakdown.value] = await Promise.all([
      dashboardApi.categoryBreakdown(year, month, "EXPENSE"),
      dashboardApi.categoryBreakdown(year, month, "INCOME"),
    ]);
  } catch (cause) {
    breakdownError.value = cause instanceof Error ? cause.message : "항목별 비교를 불러오지 못했습니다.";
  } finally {
    breakdownLoading.value = false;
  }
}

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
    dailyStart.value = period.startDate;
    dailyEnd.value = period.endDate;
    await Promise.all([loadTrend(), loadBreakdowns()]);
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

    <section class="panel-card trend-panel">
      <header class="card-heading chart-heading">
        <div><p class="eyebrow">PROFIT TREND</p><h3>손익 추이</h3></div>
        <div class="segmented-control" aria-label="조회 단위">
          <button v-for="unit in (['DAILY', 'MONTHLY', 'YEARLY'] as TrendUnit[])" :key="unit" type="button"
                  :class="{ active: trendUnit === unit }" @click="changeUnit(unit)">
            {{ { DAILY: '일별', MONTHLY: '월별', YEARLY: '연별' }[unit] }}
          </button>
        </div>
      </header>

      <form class="chart-filter" @submit.prevent="loadTrend">
        <template v-if="trendUnit === 'DAILY'">
          <label>시작일<input v-model="dailyStart" type="date" required></label>
          <label>종료일<input v-model="dailyEnd" type="date" required></label>
        </template>
        <label v-else-if="trendUnit === 'MONTHLY'">조회 연도<input v-model.number="monthlyYear" type="number" min="1" required></label>
        <template v-else>
          <label>시작 연도<input v-model.number="yearlyStart" type="number" min="1" required></label>
          <label>종료 연도<input v-model.number="yearlyEnd" type="number" min="1" required></label>
        </template>
        <button class="button secondary compact" type="submit">조회</button>
      </form>

      <div class="metric-tabs" aria-label="표시 손익">
        <button v-for="metric in (['INCOME', 'EXPENSE', 'NET_PROFIT'] as ProfitMetric[])" :key="metric" type="button"
                :class="[metric.toLowerCase(), { active: profitMetric === metric }]" @click="profitMetric = metric">
          {{ { INCOME: '수입', EXPENSE: '지출', NET_PROFIT: '순이익' }[metric] }}
        </button>
      </div>
      <StatusPanel :loading="trendLoading" :error="trendError" :empty="!trendLoading && trend.length === 0" />
      <ProfitTrendChart v-if="!trendLoading && !trendError && trend.length" :points="trend" :metric="profitMetric" :unit="trendUnit" />
    </section>

    <section class="breakdown-section">
      <header class="breakdown-toolbar">
        <div><p class="eyebrow">MONTHLY BREAKDOWN</p><h3>월별 항목 비교</h3></div>
        <label>조회 연월<input v-model="breakdownMonth" class="month-input" type="month" @change="loadBreakdowns"></label>
      </header>
      <StatusPanel :loading="breakdownLoading" :error="breakdownError" />
      <div v-if="!breakdownLoading && !breakdownError" class="breakdown-grid">
        <article class="panel-card">
          <header class="card-heading"><div><p class="eyebrow">EXPENSE</p><h3>항목별 지출</h3></div><strong>{{ currency(expenseBreakdown?.totalAmount ?? 0) }}</strong></header>
          <StatusPanel :empty="!expenseBreakdown?.items.length" empty-text="선택한 월의 지출 기록이 없습니다." />
          <CategoryBreakdownChart v-if="expenseBreakdown?.items.length" :breakdown="expenseBreakdown" color="#dd6b45" />
        </article>
        <article class="panel-card">
          <header class="card-heading"><div><p class="eyebrow">INCOME</p><h3>항목별 수입</h3></div><strong>{{ currency(incomeBreakdown?.totalAmount ?? 0) }}</strong></header>
          <StatusPanel :empty="!incomeBreakdown?.items.length" empty-text="선택한 월의 수입 기록이 없습니다." />
          <CategoryBreakdownChart v-if="incomeBreakdown?.items.length" :breakdown="incomeBreakdown" color="#287b62" />
        </article>
      </div>
    </section>

    <section class="panel-card recent-card dashboard-recent">
      <header class="card-heading"><div><p class="eyebrow">RECENT</p><h3>최근 기록</h3></div><RouterLink to="/records">전체 보기</RouterLink></header>
      <StatusPanel :empty="recent.length === 0" />
      <ul v-if="recent.length" class="record-list compact-list">
        <li v-for="record in recent" :key="record.recordId">
          <span class="record-icon" :class="record.transactionType.toLowerCase()">{{ record.transactionType === 'INCOME' ? '↗' : '↘' }}</span>
          <div><strong>{{ record.categoryName }}</strong><small>{{ shortDate(record.recordDate) }}<template v-if="record.memo"> · {{ record.memo }}</template></small></div>
          <b :class="record.transactionType.toLowerCase()">{{ record.transactionType === 'INCOME' ? '+' : '-' }}{{ currency(record.amount) }}</b>
        </li>
      </ul>
    </section>
  </template>
</template>
