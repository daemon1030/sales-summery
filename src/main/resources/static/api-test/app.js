const API = "/api/v1";
const output = document.querySelector("#response");
const methodLabel = document.querySelector("#method");
const pathLabel = document.querySelector("#request-path");
const authStatus = document.querySelector("#auth-status");

// 테스트 편의를 위해 Access Token만 브라우저 저장소에 보관한다.
let token = localStorage.getItem("sales-access-token") || "";

function setAuthStatus() {
  authStatus.textContent = token ? "JWT 저장됨" : "로그인하지 않음";
  authStatus.classList.toggle("on", Boolean(token));
}

async function request(method, path, body) {
  methodLabel.textContent = method;
  pathLabel.textContent = API + path;
  output.textContent = "요청 중...";
  const headers = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (token) headers.Authorization = `Bearer ${token}`;

  try {
    const response = await fetch(API + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    output.textContent = `${response.status} ${response.statusText}\n\n${JSON.stringify(data, null, 2)}`;
    return { response, data };
  } catch (error) {
    output.textContent = `요청 실패\n\n${error.message}`;
    return { response: null, data: null };
  }
}

function values(form) { return Object.fromEntries(new FormData(form).entries()); }
function today() { return new Date().toLocaleDateString("sv-SE", { timeZone: "Asia/Seoul" }); }
function monthStart() { const value = new Date(); value.setDate(1); return value.toLocaleDateString("sv-SE", { timeZone: "Asia/Seoul" }); }

document.querySelectorAll('input[type="date"]').forEach(input => input.value = input.name === "startDate" ? monthStart() : today());

document.querySelector("#signup-form").addEventListener("submit", event => { event.preventDefault(); request("POST", "/auth/signup", values(event.currentTarget)); });
document.querySelector("#login-form").addEventListener("submit", async event => {
  event.preventDefault();
  const result = await request("POST", "/auth/login", values(event.currentTarget));
  const accessToken = result.data?.data?.accessToken;
  if (accessToken) { token = accessToken; localStorage.setItem("sales-access-token", token); setAuthStatus(); }
});
document.querySelector("#category-form").addEventListener("submit", event => { event.preventDefault(); request("POST", "/categories", values(event.currentTarget)); });
document.querySelector("#record-form").addEventListener("submit", event => { event.preventDefault(); const body = values(event.currentTarget); body.categoryId = Number(body.categoryId); body.amount = Number(body.amount); request("POST", "/financial-records", body); });
document.querySelector("#record-search-form").addEventListener("submit", event => { event.preventDefault(); const query = new URLSearchParams(values(event.currentTarget)); request("GET", `/financial-records?${query}`); });
document.querySelector("#note-form").addEventListener("submit", event => { event.preventDefault(); request("POST", "/daily-notes", values(event.currentTarget)); });
document.querySelector("#summary-form").addEventListener("submit", event => { event.preventDefault(); const query = new URLSearchParams(values(event.currentTarget)); request("GET", `/dashboard/summary?${query}`); });
document.querySelectorAll("[data-api]").forEach(button => button.addEventListener("click", () => { const [method, path] = button.dataset.api.split(" "); request(method, path); }));
document.querySelector("#logout").addEventListener("click", () => { token = ""; localStorage.removeItem("sales-access-token"); setAuthStatus(); output.textContent = "저장된 JWT를 삭제했습니다."; });
document.querySelector("#clear").addEventListener("click", () => output.textContent = "");
setAuthStatus();
