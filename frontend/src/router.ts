import { createRouter, createWebHashHistory } from "vue-router";
import AppLayout from "./layouts/AppLayout.vue";
import AuthLayout from "./layouts/AuthLayout.vue";

export const router = createRouter({
  history: createWebHashHistory("/service/"),
  routes: [
    {
      path: "/",
      component: AppLayout,
      meta: { requiresAuth: true },
      children: [
        { path: "", redirect: "/dashboard" },
        { path: "dashboard", name: "dashboard", component: () => import("./pages/DashboardPage.vue") },
        { path: "records", name: "records", component: () => import("./pages/RecordsPage.vue") },
        { path: "imports", name: "imports", component: () => import("./pages/ImportPage.vue") },
        { path: "categories", name: "categories", component: () => import("./pages/CategoriesPage.vue") },
        { path: "daily-notes", name: "daily-notes", component: () => import("./pages/DailyNotesPage.vue") },
        { path: "settings", name: "settings", component: () => import("./pages/SettingsPage.vue") },
      ],
    },
    {
      path: "/",
      component: AuthLayout,
      meta: { guestOnly: true },
      children: [
        { path: "login", name: "login", component: () => import("./pages/LoginPage.vue") },
        { path: "signup", name: "signup", component: () => import("./pages/SignupPage.vue") },
      ],
    },
    { path: "/:pathMatch(.*)*", name: "not-found", component: () => import("./pages/NotFoundPage.vue") },
  ],
});
