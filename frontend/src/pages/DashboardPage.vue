<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
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
const week = ref<ProfitSummary>({ totalIncome: 0, totalExpense: 0, netProfit: 0 });
const settlement = ref<PeriodSummary | null>(null);
const recent = ref<FinancialRecord[]>([]);
type SummaryPeriod = "DAY" | "WEEK" | "SETTLEMENT";
const summaryPeriod = ref<SummaryPeriod>("DAY");

const currentDate = todayInKorea();
const currentWeek = weekRange(currentDate);
const selectedSummary = computed(() => {
  if (summaryPeriod.value === "WEEK") return week.value;
  if (summaryPeriod.value === "SETTLEMENT") return settlement.value?.summary ?? today.value;
  return today.value;
});
const summaryPeriodName = computed(() => ({
  DAY: "오늘",
  WEEK: "이번 주",
  SETTLEMENT: "정산 기간",
}[summaryPeriod.value]));
const summaryRangeLabel = computed(() => {
  if (summaryPeriod.value === "WEEK") return `${currentWeek.startDate} — ${currentWeek.endDate}`;
  if (summaryPeriod.value === "SETTLEMENT" && settlement.value) {
    return `${settlement.value.startDate} — ${settlement.value.endDate}`;
  }
  return currentDate;
});

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
const breakdownRangeLabel = computed(() => incomeBreakdown.value
  ? `${incomeBreakdown.value.startDate} — ${incomeBreakdown.value.endDate}`
  : "");
const breakdownSummary = computed(() => {
  const totalIncome = Number(incomeBreakdown.value?.totalAmount ?? 0);
  const totalExpense = Number(expenseBreakdown.value?.totalAmount ?? 0);
  const netProfit = totalIncome - totalExpense;
  return {
    totalIncome,
    totalExpense,
    netProfit,
    incomeRate: totalIncome > 0 ? 100 : null,
    expenseRate: totalIncome > 0 ? (totalExpense / totalIncome) * 100 : null,
    marginRate: totalIncome > 0 ? (netProfit / totalIncome) * 100 : null,
  };
});

const rate = (value: number | null) => value === null ? "—" : `${new Intl.NumberFormat("ko-KR", {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
}).format(value)}%`;

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
    const [todayData, weekData, period, recentData] = await Promise.all([
      dashboardApi.today(),
      dashboardApi.summary(currentWeek.startDate, currentWeek.endDate),
      dashboardApi.current(),
      dashboardApi.recent(),
    ]);
    today.value = todayData;
    week.value = weekData;
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

function weekRange(dateValue: string) {
  const date = new Date(`${dateValue}T00:00:00Z`);
  const mondayOffset = (date.getUTCDay() + 6) % 7;
  return {
    startDate: shiftDate(dateValue, -mondayOffset),
    endDate: shiftDate(dateValue, 6 - mondayOffset),
  };
}

function shiftDate(dateValue: string, days: number) {
  const date = new Date(`${dateValue}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
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
    <section class="summary-period-bar">
      <div><p class="eyebrow">PERIOD SUMMARY</p><strong>{{ summaryRangeLabel }}</strong></div>
      <div class="segmented-control" aria-label="요약 조회 기간">
        <button v-for="period in (['DAY', 'WEEK', 'SETTLEMENT'] as SummaryPeriod[])" :key="period" type="button"
                :class="{ active: summaryPeriod === period }" @click="summaryPeriod = period">
          {{ { DAY: '일', WEEK: '주', SETTLEMENT: '월(정산기간)' }[period] }}
        </button>
      </div>
    </section>
    <section class="summary-grid">
      <article class="summary-card income"><span>{{ summaryPeriodName }} 총수입</span><strong>{{ currency(selectedSummary.totalIncome) }}</strong><small>선택한 기간의 수입 합계</small></article>
      <article class="summary-card expense"><span>{{ summaryPeriodName }} 총지출</span><strong>{{ currency(selectedSummary.totalExpense) }}</strong><small>선택한 기간의 지출 합계</small></article>
      <article class="summary-card profit"><span>{{ summaryPeriodName }} 순이익</span><strong>{{ currency(selectedSummary.netProfit) }}</strong><small>수입에서 지출을 제외한 금액</small></article>
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
        <div><p class="eyebrow">SETTLEMENT BREAKDOWN</p><h3>정산 월별 항목 비교</h3><p v-if="breakdownRangeLabel" class="breakdown-range">{{ breakdownRangeLabel }}</p></div>
        <label>조회 정산월<input v-model="breakdownMonth" class="month-input" type="month" @change="loadBreakdowns"></label>
      </header>
      <StatusPanel :loading="breakdownLoading" :error="breakdownError" />
      <div v-if="!breakdownLoading && !breakdownError" class="breakdown-summary-grid">
        <article class="income">
          <span>수입 기준</span><strong>{{ rate(breakdownSummary.incomeRate) }}</strong>
          <small>{{ currency(breakdownSummary.totalIncome) }}</small>
        </article>
        <article class="expense">
          <span>지출률</span><strong>{{ rate(breakdownSummary.expenseRate) }}</strong>
          <small>{{ currency(breakdownSummary.totalExpense) }}</small>
        </article>
        <article class="margin" :class="{ negative: breakdownSummary.netProfit < 0 }">
          <span>마진율</span><strong>{{ rate(breakdownSummary.marginRate) }}</strong>
          <small>순이익 {{ currency(breakdownSummary.netProfit) }}</small>
        </article>
      </div>
      <div v-if="!breakdownLoading && !breakdownError" class="breakdown-grid">
        <article class="panel-card">
          <header class="card-heading"><div><p class="eyebrow">EXPENSE</p><h3>항목별 지출</h3></div><strong>{{ currency(expenseBreakdown?.totalAmount ?? 0) }}</strong></header>
          <StatusPanel :empty="!expenseBreakdown?.items.length" empty-text="선택한 정산 기간의 지출 기록이 없습니다." />
          <CategoryBreakdownChart v-if="expenseBreakdown?.items.length" :breakdown="expenseBreakdown" color="#dd6b45" />
        </article>
        <article class="panel-card">
          <header class="card-heading"><div><p class="eyebrow">INCOME</p><h3>항목별 수입</h3></div><strong>{{ currency(incomeBreakdown?.totalAmount ?? 0) }}</strong></header>
          <StatusPanel :empty="!incomeBreakdown?.items.length" empty-text="선택한 정산 기간의 수입 기록이 없습니다." />
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
