import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { authApi, userApi } from "../api";
import { TOKEN_KEY } from "../api/client";
import type { User } from "../api/types";

export const useAuthStore = defineStore("auth", () => {
  const user = ref<User | null>(null);
  const initialized = ref(false);
  const isAuthenticated = computed(() => Boolean(user.value && sessionStorage.getItem(TOKEN_KEY)));

  async function login(loginId: string, password: string) {
    const loginResponse = await authApi.login({ loginId, password });
    sessionStorage.setItem(TOKEN_KEY, loginResponse.accessToken);
    user.value = await userApi.me();
  }

  async function restoreSession() {
    try {
      if (sessionStorage.getItem(TOKEN_KEY)) user.value = await userApi.me();
    } catch {
      clearSession();
    } finally {
      initialized.value = true;
    }
  }

  function clearSession() {
    sessionStorage.removeItem(TOKEN_KEY);
    user.value = null;
    initialized.value = true;
  }

  function updateUser(next: User) { user.value = next; }

  return { user, initialized, isAuthenticated, login, restoreSession, clearSession, updateUser };
});
