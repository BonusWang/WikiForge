<script setup lang="ts">
import { DocumentChecked, Files, Refresh } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, ref } from 'vue';
import { listImportJobs } from '../../api/import-jobs';
import { listWikiIngestRuns } from '../../api/wiki-ingest-runs';
import {
  getImportJobStatusDisplay,
  getWikiWriteStatusDisplay
} from '../../features/status/statusDisplay';
import type { ImportJob } from '../../types/importJobs';
import type { WikiIngestRun } from '../../types/wikiIngestRuns';

const jobs = ref<ImportJob[]>([]);
const runs = ref<WikiIngestRun[]>([]);
const loading = ref(false);

const failedJobCount = computed(() => jobs.value.filter((job) => {
  return (job.statusCode || job.status) === '失败' || job.status === 'failed';
}).length);
const failedRunCount = computed(() => runs.value.filter((run) => {
  return (run.writeStatusCode || run.statusCode) === '失败';
}).length);

async function refreshLogs() {
  loading.value = true;
  try {
    const [jobResult, runResult] = await Promise.all([
      listImportJobs({ page: 1, pageSize: 20 }),
      listWikiIngestRuns({ page: 1, pageSize: 20 })
    ]);
    jobs.value = jobResult.items;
    runs.value = runResult.items;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '运行日志加载失败');
  } finally {
    loading.value = false;
  }
}

onMounted(refreshLogs);
</script>

<template>
  <div class="mvp0-stack">
    <div class="status-grid">
      <el-card shadow="never">
        <template #header>
          <div class="card-title">
            <el-icon><Files /></el-icon>
            收纳任务
          </div>
        </template>
        <p class="metric ok">{{ jobs.length }}</p>
        <p class="muted">最近 20 条文件收纳记录</p>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="card-title">
            <el-icon><DocumentChecked /></el-icon>
            Wiki 写入
          </div>
        </template>
        <p class="metric ok">{{ runs.length }}</p>
        <p class="muted">最近 20 条 Obsidian 写入记录</p>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="card-title">失败记录</div>
        </template>
        <p class="metric">{{ failedJobCount + failedRunCount }}</p>
        <p class="muted">收纳失败与 Wiki 写入失败汇总</p>
      </el-card>
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="section-title">
          <div class="card-title">收纳运行日志</div>
          <el-button :loading="loading" @click="refreshLogs">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="jobs" border empty-text="暂无收纳日志">
        <el-table-column prop="createdAt" label="创建时间" min-width="180" show-overflow-tooltip />
        <el-table-column prop="jobUid" label="任务编号" min-width="190" show-overflow-tooltip />
        <el-table-column label="来源" min-width="260" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.inputPathMasked || scope.row.inputPath || scope.row.importType }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag
              :class="getImportJobStatusDisplay(scope.row.statusCode || scope.row.status).className"
              :type="getImportJobStatusDisplay(scope.row.statusCode || scope.row.status).tagType"
              effect="plain"
            >
              {{ getImportJobStatusDisplay(scope.row.statusCode || scope.row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="总数" width="86" align="right" />
        <el-table-column prop="successCount" label="成功" width="86" align="right" />
        <el-table-column prop="skippedCount" label="跳过" width="86" align="right" />
        <el-table-column prop="failedCount" label="失败" width="86" align="right" />
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="section-title">
          <div class="card-title">Wiki 写入日志</div>
        </div>
      </template>

      <el-table v-loading="loading" :data="runs" border empty-text="暂无 Wiki 写入日志">
        <el-table-column prop="createdAt" label="创建时间" min-width="180" show-overflow-tooltip />
        <el-table-column prop="runUid" label="运行编号" min-width="190" show-overflow-tooltip />
        <el-table-column prop="fileName" label="资料" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="130">
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
        <el-table-column prop="sourcePagePath" label="来源页" min-width="220" show-overflow-tooltip />
        <el-table-column label="Wiki 页" min-width="220" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.wikiPagePaths?.join('，') || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="fallbackReason" label="兜底原因" min-width="220" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>
