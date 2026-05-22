<script setup lang="ts">
import { Connection, Cpu, Files, SetUp } from '@element-plus/icons-vue';
import { onMounted, ref } from 'vue';
import { useAppStore } from '../stores/app';
import { fetchBackendHealth, type BackendHealth } from '../services/http';

const appStore = useAppStore();
const backendHealth = ref<BackendHealth | null>(null);
const loading = ref(false);
const errorMessage = ref('');

async function refreshHealth() {
  loading.value = true;
  errorMessage.value = '';
  try {
    backendHealth.value = await fetchBackendHealth();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '无法连接后端服务';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void refreshHealth();
});
</script>

<template>
  <main class="shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">{{ appStore.stage }}</p>
        <h1>{{ appStore.appName }}</h1>
      </div>
      <el-button :loading="loading" type="primary" @click="refreshHealth">
        刷新状态
      </el-button>
    </header>

    <section class="status-grid">
      <el-card shadow="never">
        <template #header>
          <div class="card-title">
            <el-icon><Connection /></el-icon>
            后端服务
          </div>
        </template>
        <p class="metric" :class="{ ok: backendHealth?.status === 'UP' }">
          {{ backendHealth?.status ?? 'UNKNOWN' }}
        </p>
        <p class="muted">{{ backendHealth?.service ?? '等待连接' }}</p>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="card-title">
            <el-icon><SetUp /></el-icon>
            工程阶段
          </div>
        </template>
        <p class="metric">MVP 0</p>
        <p class="muted">项目骨架 / CI / Docker / Flyway</p>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="card-title">
            <el-icon><Cpu /></el-icon>
            数据库
          </div>
        </template>
        <p class="metric">MySQL</p>
        <p class="muted">Flyway 管理 MVP 0 表结构</p>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="card-title">
            <el-icon><Files /></el-icon>
            知识层
          </div>
        </template>
        <p class="metric">待接入</p>
        <p class="muted">Raw Sources 与 Obsidian 将在 MVP 1/2 实现</p>
      </el-card>
    </section>

    <el-alert
      v-if="errorMessage"
      class="alert"
      :title="errorMessage"
      type="warning"
      show-icon
      :closable="false"
    />
  </main>
</template>
