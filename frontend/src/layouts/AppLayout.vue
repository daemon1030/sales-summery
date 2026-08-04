<script setup lang="ts">
import { computed } from "vue";
import { RouterLink, RouterView, useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const navigation = [
  { to: "/dashboard", label: "대시보드", icon: "⌂" },
  { to: "/records", label: "기록", icon: "↕" },
  { to: "/categories", label: "항목", icon: "▦" },
  { to: "/daily-notes", label: "특이사항", icon: "□" },
  { to: "/settings", label: "설정", icon: "⚙" },
];

const title = computed(() => navigation.find(item => route.path.startsWith(item.to))?.label ?? "Sales Summary");

function logout() {
  auth.clearSession();
  router.push({ name: "login" });
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="sidebar-brand"><span class="brand-mark small">S</span><strong>Sales<br>Summary</strong></div>
      <nav aria-label="주요 메뉴">
        <RouterLink v-for="item in navigation" :key="item.to" :to="item.to">
          <span class="nav-icon">{{ item.icon }}</span><span>{{ item.label }}</span>
        </RouterLink>
      </nav>
      <div class="sidebar-user">
        <span class="avatar">{{ auth.user?.name?.slice(0, 1) }}</span>
        <div><strong>{{ auth.user?.name }}</strong><small>{{ auth.user?.loginId }}</small></div>
        <button class="icon-button" title="로그아웃" @click="logout">↪</button>
      </div>
    </aside>

    <div class="app-content">
      <header class="topbar">
        <div><p class="eyebrow">SALES SUMMARY</p><h1>{{ title }}</h1></div>
        <div class="topbar-actions"><span>{{ auth.user?.name }}님</span><button class="button secondary compact" @click="logout">로그아웃</button></div>
      </header>
      <main class="page-content"><RouterView /></main>
    </div>

    <nav class="mobile-nav" aria-label="모바일 주요 메뉴">
      <RouterLink v-for="item in navigation" :key="item.to" :to="item.to">
        <span>{{ item.icon }}</span><small>{{ item.label }}</small>
      </RouterLink>
    </nav>
  </div>
</template>
