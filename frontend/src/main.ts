import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import { router } from "./router";
import { useAuthStore } from "./stores/auth";
import "./styles.css";

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);

const auth = useAuthStore(pinia);

router.beforeEach(async (to) => {
  if (!auth.initialized) await auth.restoreSession();
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: "login", query: { redirect: to.fullPath } };
  }
  if (to.meta.guestOnly && auth.isAuthenticated) return { name: "dashboard" };
  return true;
});

window.addEventListener("auth:expired", () => {
  auth.clearSession();
  if (router.currentRoute.value.meta.requiresAuth) router.push({ name: "login" });
});

app.mount("#app");
