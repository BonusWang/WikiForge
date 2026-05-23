<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { fetchOverview, fetchTask, fetchTasks } from './api';
import type { OrchestrationOverview, OrchestrationTask } from './types';

const overview = ref<OrchestrationOverview | null>(null);
const tasks = ref<OrchestrationTask[]>([]);
const selectedTask = ref<OrchestrationTask | null>(null);
const loading = ref(false);
const errorMessage = ref('');

const statusOrder = ['Doing', 'Ready', 'Review', 'Blocked', 'Done'];

const orderedTasks = computed(() => {
  return [...tasks.value].sort((left, right) => {
    const leftIndex = statusOrder.indexOf(left.status);
    const rightIndex = statusOrder.indexOf(right.status);
    return (leftIndex === -1 ? 99 : leftIndex) - (rightIndex === -1 ? 99 : rightIndex);
  });
});

async function refresh() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [nextOverview, nextTasks] = await Promise.all([fetchOverview(), fetchTasks()]);
    overview.value = nextOverview;
    tasks.value = nextTasks;
    if (!selectedTask.value && nextTasks.length > 0) {
      selectedTask.value = nextTasks[0];
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Orchestration service unavailable';
  } finally {
    loading.value = false;
  }
}

async function selectTask(task: OrchestrationTask) {
  selectedTask.value = task;
  try {
    selectedTask.value = await fetchTask(task.taskId);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Task detail unavailable';
  }
}

function statusClass(status: string) {
  return `status-${status.toLowerCase()}`;
}

onMounted(() => {
  void refresh();
});
</script>

<template>
  <main class="app-shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">WikiForge Orchestration</p>
        <h1>开发编排控制台</h1>
      </div>
      <button class="refresh-button" :disabled="loading" @click="refresh">
        {{ loading ? '刷新中' : '刷新' }}
      </button>
    </header>

    <section class="overview-band">
      <div>
        <span class="label">当前阶段</span>
        <strong>{{ overview?.currentStage || '等待服务连接' }}</strong>
      </div>
      <div>
        <span class="label">当前分支</span>
        <strong>{{ overview?.activeBranch || '-' }}</strong>
      </div>
      <div>
        <span class="label">任务来源</span>
        <strong>{{ overview?.source || '-' }}</strong>
      </div>
    </section>

    <p v-if="errorMessage" class="error-line">{{ errorMessage }}</p>

    <section class="metrics-grid">
      <div class="metric-tile">
        <span>Total</span>
        <strong>{{ overview?.stats.total ?? 0 }}</strong>
      </div>
      <div class="metric-tile">
        <span>Doing</span>
        <strong>{{ overview?.stats.doing ?? 0 }}</strong>
      </div>
      <div class="metric-tile">
        <span>Ready</span>
        <strong>{{ overview?.stats.ready ?? 0 }}</strong>
      </div>
      <div class="metric-tile">
        <span>Done</span>
        <strong>{{ overview?.stats.done ?? 0 }}</strong>
      </div>
    </section>

    <section class="workspace-grid">
      <div class="task-list">
        <div class="section-head">
          <h2>任务队列</h2>
          <span>{{ tasks.length }} items</span>
        </div>

        <button
          v-for="task in orderedTasks"
          :key="task.taskId"
          class="task-row"
          :class="{ active: selectedTask?.taskId === task.taskId }"
          @click="selectTask(task)"
        >
          <span class="task-id">{{ task.taskId }}</span>
          <span class="task-title">{{ task.title }}</span>
          <span class="status-pill" :class="statusClass(task.status)">{{ task.status }}</span>
        </button>
      </div>

      <article class="detail-pane">
        <template v-if="selectedTask">
          <div class="section-head">
            <div>
              <p class="eyebrow">{{ selectedTask.parentTask }}</p>
              <h2>{{ selectedTask.title }}</h2>
            </div>
            <span class="status-pill" :class="statusClass(selectedTask.status)">{{ selectedTask.status }}</span>
          </div>

          <dl class="detail-grid">
            <div>
              <dt>Owner</dt>
              <dd>{{ selectedTask.owner }}</dd>
            </div>
            <div>
              <dt>Scope</dt>
              <dd>{{ selectedTask.scope }}</dd>
            </div>
            <div>
              <dt>Goal</dt>
              <dd>{{ selectedTask.goal }}</dd>
            </div>
            <div>
              <dt>Next</dt>
              <dd>{{ selectedTask.nextStep }}</dd>
            </div>
          </dl>

          <div class="detail-columns">
            <section>
              <h3>允许修改</h3>
              <ul>
                <li v-for="item in selectedTask.allowedFiles" :key="item">{{ item }}</li>
              </ul>
            </section>
            <section>
              <h3>禁止修改</h3>
              <ul>
                <li v-for="item in selectedTask.forbiddenFiles" :key="item">{{ item }}</li>
              </ul>
            </section>
          </div>

          <section>
            <h3>契约</h3>
            <ul class="dense-list">
              <li v-for="item in selectedTask.contracts" :key="item">{{ item }}</li>
            </ul>
          </section>

          <section>
            <h3>验收命令</h3>
            <pre>{{ selectedTask.verificationCommands.join('\n') }}</pre>
          </section>

          <section>
            <h3>Handoff</h3>
            <p>{{ selectedTask.handoff }}</p>
          </section>
        </template>
        <template v-else>
          <p class="empty-state">选择一个任务查看详情。</p>
        </template>
      </article>
    </section>
  </main>
</template>
