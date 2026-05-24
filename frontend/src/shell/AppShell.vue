<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue';
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import MainNavigation from './MainNavigation.vue';
import { fetchBackendHealth, type BackendHealth } from '../services/http';
import { useAppStore } from '../stores/app';

const route = useRoute();
const appStore = useAppStore();
const backendHealth = ref<BackendHealth | null>(null);
const healthLoading = ref(false);

const pageTitle = computed(() => String(route.meta.title || 'WikiForge'));
const pageSubtitle = computed(() => String(route.meta.subtitle || 'MVP0 私有知识库收纳闭环'));
const healthLabel = computed(() => {
  if (!backendHealth.value) {
    return '未连接';
  }
  return backendHealth.value.status === 'UP' ? '后端正常' : backendHealth.value.status;
});

async function refreshHealth() {
  healthLoading.value = true;
  try {
    backendHealth.value = await fetchBackendHealth();
  } catch {
    backendHealth.value = null;
  } finally {
    healthLoading.value = false;
  }
}

onMounted(refreshHealth);
</script>

<template>
  <div class="app-shell">
    <MainNavigation />

    <main class="shell workspace">
      <section class="topbar">
        <div>
          <span class="eyebrow">{{ appStore.stage }}</span>
          <h1>{{ pageTitle }}</h1>
          <p class="page-subtitle">{{ pageSubtitle }}</p>
        </div>

        <div class="topbar-actions">
          <el-tag :type="backendHealth ? 'success' : 'warning'" effect="plain">
            {{ healthLabel }}
          </el-tag>
          <el-button :loading="healthLoading" @click="refreshHealth">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </section>

      <RouterView />
    </main>
  </div>
</template>
