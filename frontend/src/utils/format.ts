export const currency = (value: number | string) => new Intl.NumberFormat("ko-KR", {
  style: "currency",
  currency: "KRW",
  maximumFractionDigits: 0,
}).format(Number(value));

export const shortDate = (value: string) => new Intl.DateTimeFormat("ko-KR", {
  month: "short",
  day: "numeric",
  weekday: "short",
}).format(new Date(`${value}T00:00:00`));

export const todayInKorea = () => new Date().toLocaleDateString("sv-SE", { timeZone: "Asia/Seoul" });

export function monthRange() {
  const today = todayInKorea();
  return { startDate: `${today.slice(0, 7)}-01`, endDate: today };
}
