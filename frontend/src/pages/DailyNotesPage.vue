<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { noteApi } from "../api";
import type { DailyNote } from "../api/types";
import StatusPanel from "../components/StatusPanel.vue";
import { shortDate, todayInKorea } from "../utils/format";

const month = ref(todayInKorea().slice(0, 7));
const notes = ref<DailyNote[]>([]);
const loading = ref(true);
const error = ref("");
const saving = ref(false);
const editingId = ref<number | null>(null);
const noteDate = ref(todayInKorea());
const content = ref("");
const formError = ref("");

const range = computed(() => {
  const [year, monthNumber] = month.value.split("-").map(Number);
  const lastDay = new Date(year, monthNumber, 0).getDate();
  return { startDate: `${month.value}-01`, endDate: `${month.value}-${String(lastDay).padStart(2, "0")}` };
});

async function load() {
  loading.value = true;
  error.value = "";
  try { notes.value = await noteApi.list(range.value.startDate, range.value.endDate); }
  catch (cause) { error.value = cause instanceof Error ? cause.message : "특이사항을 불러오지 못했습니다."; }
  finally { loading.value = false; }
}

function edit(note: DailyNote) {
  editingId.value = note.noteId;
  noteDate.value = note.noteDate;
  content.value = note.content;
  formError.value = "";
}

function resetForm() {
  editingId.value = null;
  noteDate.value = todayInKorea();
  content.value = "";
  formError.value = "";
}

async function save() {
  saving.value = true;
  formError.value = "";
  try {
    if (editingId.value) await noteApi.update(editingId.value, content.value.trim());
    else await noteApi.create({ noteDate: noteDate.value, content: content.value.trim() });
    resetForm();
    await load();
  } catch (cause) { formError.value = cause instanceof Error ? cause.message : "특이사항을 저장하지 못했습니다."; }
  finally { saving.value = false; }
}

async function remove(note: DailyNote) {
  if (!window.confirm(`${note.noteDate} 특이사항을 삭제할까요?`)) return;
  try { await noteApi.remove(note.noteId); if (editingId.value === note.noteId) resetForm(); await load(); }
  catch (cause) { error.value = cause instanceof Error ? cause.message : "특이사항을 삭제하지 못했습니다."; }
}

onMounted(load);
</script>

<template>
  <section class="page-heading">
    <div><p class="eyebrow">DAILY NOTES</p><h2>날짜별 특이사항</h2><p>매출 숫자만으로 설명되지 않는 하루의 상황을 남겨두세요.</p></div>
  </section>

  <section class="notes-layout">
    <article class="panel-card notes-list-card">
      <header class="card-heading"><div><p class="eyebrow">MONTHLY NOTES</p><h3>{{ month.replace('-', '년 ') }}월</h3></div><input v-model="month" class="month-input" type="month" @change="load"></header>
      <StatusPanel :loading="loading" :error="error" :empty="!loading && !error && notes.length === 0" empty-text="오른쪽 입력창에서 이달의 첫 특이사항을 등록해 보세요." />
      <ul v-if="notes.length" class="note-list">
        <li v-for="note in notes" :key="note.noteId" :class="{ selected: editingId === note.noteId }">
          <button class="note-main" @click="edit(note)"><time>{{ shortDate(note.noteDate) }}</time><strong>{{ note.content }}</strong></button>
          <button class="icon-button danger" aria-label="삭제" @click="remove(note)">×</button>
        </li>
      </ul>
    </article>

    <article class="panel-card note-editor">
      <header class="card-heading"><div><p class="eyebrow">{{ editingId ? 'EDIT NOTE' : 'NEW NOTE' }}</p><h3>{{ editingId ? '특이사항 수정' : '특이사항 등록' }}</h3></div><button v-if="editingId" class="text-button" @click="resetForm">새로 작성</button></header>
      <form class="form-stack" @submit.prevent="save">
        <label>날짜<input v-model="noteDate" type="date" :disabled="Boolean(editingId)" required></label>
        <label>내용<textarea v-model="content" rows="9" required placeholder="예약, 행사, 날씨, 재고 이슈 등 오늘의 상황을 기록하세요."></textarea></label>
        <p v-if="formError" class="form-error">{{ formError }}</p>
        <button class="button primary wide" :disabled="saving || !content.trim()">{{ saving ? '저장 중…' : editingId ? '수정 저장' : '특이사항 등록' }}</button>
      </form>
    </article>
  </section>
</template>
