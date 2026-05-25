<script setup lang="ts">
import { Collection, Document, Refresh } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, ref } from 'vue';
import { listWikiIngestRuns } from '../../api/wiki-ingest-runs';
import { getWikiWriteStatusDisplay } from '../../features/status/statusDisplay';
import type { WikiIngestRun } from '../../types/wikiIngestRuns';

const runs = ref<WikiIngestRun[]>([]);
const loading = ref(false);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);

const sourcePageCount = computed(() => runs.value.filter((run) => run.sourcePagePath).length);
const indexLogCount = computed(() => {
  return runs.value.filter((run) => run.indexUpdated || run.logEntryAppended).length;
});

async function refreshRuns() {
  loading.value = true;
  try {
    const result = await listWikiIngestRuns({
      page: page.value,
      pageSize: pageSize.value
    });
    runs.value = result.items;
    total.value = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Wiki 写入记录加载失败');
  } finally {
    loading.value = false;
  }
}

async function handlePageChange(nextPage: number) {
  page.value = nextPage;
  await refreshRuns();
}

onMounted(refreshRuns);
</script>

<template>
  <div class="mvp0-stack">
    <div class="status-grid">
      <el-card shadow="never">
        <template #header>
          <div class="card-title">
            <el-icon><Document /></el-icon>
            来源页
          </div>
        </template>
        <p class="metric ok">{{ sourcePageCount }}</p>
        <p class="muted">资料仓库对应的托管说明页</p>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="card-title">
            <el-icon><Collection /></el-icon>
            Wiki
          </div>
        </template>
        <p class="metric ok">{{ total }}</p>
        <p class="muted">由 LLM Wiki 规则维护的整理层</p>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="card-title">
            <el-icon><Refresh /></el-icon>
            index/log
          </div>
        </template>
        <p class="metric">{{ indexLogCount }}</p>
        <p class="muted">index.md 与 log.md 写入记录</p>
      </el-card>
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="section-title">
          <div class="card-title">Wiki 写入记录</div>
          <el-button :loading="loading" @click="refreshRuns">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="runs" border empty-text="暂无 Wiki 写入记录">
        <el-table-column prop="createdAt" label="创建时间" min-width="180" show-overflow-tooltip />
        <el-table-column prop="fileName" label="资料" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sourcePagePath" label="来源页" min-width="220" show-overflow-tooltip />
        <el-table-column label="index/log" min-width="150">
          <template #default="scope">
            <span class="detail-item">
              index {{ scope.row.indexUpdated ? '已更新' : '未更新' }} /
              log {{ scope.row.logEntryAppended ? '已记录' : '未记录' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="写入状态" width="130">
          <template #default="scope">
            <el-tag
              :class="getWikiWriteStatusDisplay(scope.row.writeStatusCode || scope.row.statusCode).className"
              :type="getWikiWriteStatusDisplay(scope.row.writeStatusCode || scope.row.statusCode).tagType"
              effect="plain"
            >
              {{ getWikiWriteStatusDisplay(scope.row.writeStatusCode || scope.row.statusCode).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failureReason" label="失败原因" min-width="220" show-overflow-tooltip />
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-if="total > pageSize"
          background
          layout="prev, pager, next"
          :current-page="page"
          :page-size="pageSize"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>
