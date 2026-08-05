<script setup lang="ts">
import { computed } from "vue";
import { Line } from "vue-chartjs";
import {
  CategoryScale, Chart as ChartJS, Filler, Legend, LinearScale,
  LineElement, PointElement, Tooltip, type ChartOptions,
} from "chart.js";
import type { ProfitMetric, ProfitTrendPoint, TrendUnit } from "../api/types";
import { currency } from "../utils/format";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend, Filler);

const props = defineProps<{
  points: ProfitTrendPoint[];
  metric: ProfitMetric;
  unit: TrendUnit;
}>();

const metricConfig = computed(() => ({
  INCOME: { key: "totalIncome" as const, label: "수입", color: "#287b62", fill: "rgba(40, 123, 98, .12)" },
  EXPENSE: { key: "totalExpense" as const, label: "지출", color: "#dd6b45", fill: "rgba(221, 107, 69, .12)" },
  NET_PROFIT: { key: "netProfit" as const, label: "순이익", color: "#194c3f", fill: "rgba(25, 76, 63, .10)" },
}[props.metric]));

const label = (period: string) => {
  if (props.unit === "DAILY") return `${Number(period.slice(5, 7))}/${Number(period.slice(8, 10))}`;
  if (props.unit === "MONTHLY") return `${Number(period.slice(5, 7))}월`;
  return `${period}년`;
};

const chartData = computed(() => ({
  labels: props.points.map(item => label(item.period)),
  datasets: [{
    label: metricConfig.value.label,
    data: props.points.map(item => Number(item[metricConfig.value.key])),
    borderColor: metricConfig.value.color,
    backgroundColor: metricConfig.value.fill,
    borderWidth: 3,
    pointRadius: props.points.length > 40 ? 0 : 3,
    pointHoverRadius: 5,
    tension: .28,
    fill: true,
  }],
}));

const compactAmount = (value: number) => new Intl.NumberFormat("ko-KR", {
  notation: "compact",
  maximumFractionDigits: 1,
}).format(value);

const options: ChartOptions<"line"> = {
  responsive: true,
  maintainAspectRatio: false,
  interaction: { intersect: false, mode: "index" },
  plugins: {
    legend: { display: false },
    tooltip: { callbacks: { label: context => `${context.dataset.label}: ${currency(context.parsed.y ?? 0)}` } },
  },
  scales: {
    x: { grid: { display: false }, ticks: { maxTicksLimit: 9, maxRotation: 0 } },
    y: {
      beginAtZero: true,
      ticks: { callback: value => compactAmount(Number(value)) },
      grid: { color: "rgba(107, 116, 111, .13)" },
    },
  },
};
</script>

<template>
  <div class="trend-chart"><Line :data="chartData" :options="options" /></div>
</template>
