<script setup lang="ts">
import { computed } from "vue";
import { Bar } from "vue-chartjs";
import {
  BarElement, CategoryScale, Chart as ChartJS, Legend, LinearScale,
  Tooltip, type ChartOptions,
} from "chart.js";
import type { CategoryBreakdown } from "../api/types";
import { currency } from "../utils/format";

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip, Legend);

const props = defineProps<{ breakdown: CategoryBreakdown; color: string }>();

const chartData = computed(() => ({
  labels: props.breakdown.items.map(item => `${item.categoryName}  ${Number(item.percentage).toFixed(1)}%`),
  datasets: [{
    label: props.breakdown.transactionType === "INCOME" ? "수입" : "지출",
    data: props.breakdown.items.map(item => Number(item.amount)),
    backgroundColor: props.color,
    borderRadius: 7,
    borderSkipped: false,
    barThickness: 22,
  }],
}));

const compactAmount = (value: number) => new Intl.NumberFormat("ko-KR", {
  notation: "compact",
  maximumFractionDigits: 1,
}).format(value);

const options: ChartOptions<"bar"> = {
  indexAxis: "y",
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        label: context => {
          const item = props.breakdown.items[context.dataIndex];
          return `${currency(item.amount)} · ${Number(item.percentage).toFixed(1)}%`;
        },
      },
    },
  },
  scales: {
    x: {
      beginAtZero: true,
      ticks: { callback: value => compactAmount(Number(value)) },
      grid: { color: "rgba(107, 116, 111, .13)" },
    },
    y: { grid: { display: false } },
  },
};

const chartHeight = computed(() => `${Math.max(210, props.breakdown.items.length * 48)}px`);
</script>

<template>
  <div class="breakdown-chart" :style="{ height: chartHeight }">
    <Bar :data="chartData" :options="options" />
  </div>
</template>
