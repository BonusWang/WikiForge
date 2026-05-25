<script setup lang="ts">
import { Files, Refresh } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { onMounted, ref } from 'vue';
import { getImportJob, listImportJobs } from '../../api/import-jobs';
import { listSourceFiles } from '../../api/source-files';
import { createWikiIngestRun } from '../../api/wiki-ingest-runs';
import {
  formatBytes,
  getImportJobStatusDisplay,
  importJobStatusOptions
} from '../../features/status/statusDisplay';
import type {
  ImportJob,
  ImportJobDetail,
  SourceFile
} from '../../types/importJobs';

const statusFilter = ref('');
const jobs = ref<ImportJob[]>([]);
const selectedJobUid = ref('');
const selectedJobDetail = ref<ImportJobDetail | null>(null);
const sourceFiles = ref<SourceFile[]>([]);
const jobLoading = ref(false);
const detailLoading = ref(false);
const sourceLoading = ref(false);
const wikiWritingFileUid = ref('');
const jobPage = ref(1);
const jobPageSize = ref(20);
const jobTotal = ref(0);
const sourcePage = ref(1);
const sourcePageSize = ref(50);
const sourceTotal = ref(0);

async function refreshJobs() {
  jobLoading.value = true;
  try {
    const result = await listImportJobs({
      statusCode: statusFilter.value || undefined,
      page: jobPage.value,
      pageSize: jobPageSize.value
    });
    jobs.value = result.items;
    jobTotal.value = result.total;
    if (!selectedJobUid.value && result.items.length > 0) {
      await selectJob(result.items[0]);
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '任务列表加载失败');
  } finally {
    jobLoading.value = false;
  }
}

async function refreshJobDetail() {
  if (!selectedJobUid.value) {
    selectedJobDetail.value = null;
    return;
  }

  detailLoading.value = true;
  try {
    selectedJobDetail.value = await getImportJob(selectedJobUid.value);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '任务详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

async function refreshSourceFiles() {
  if (!selectedJobUid.value) {
    sourceFiles.value = [];
    sourceTotal.value = 0;
    return;
  }

  sourceLoading.value = true;
  try {
    const result = await listSourceFiles({
      jobUid: selectedJobUid.value,
      page: sourcePage.value,
      pageSize: sourcePageSize.value
    });
    sourceFiles.value = result.items;
    sourceTotal.value = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '资料文件加载失败');
  } finally {
    sourceLoading.value = false;
  }
}

async function selectJob(job: ImportJob) {
  selectedJobUid.value = job.jobUid;
  sourcePage.value = 1;
  await Promise.all([refreshJobDetail(), refreshSourceFiles()]);
}

async function refreshSelectedJob() {
  await Promise.all([refreshJobDetail(), refreshSourceFiles()]);
}

async function handleJobPageChange(page: number) {
  jobPage.value = page;
  await refreshJobs();
}

async function handleSourcePageChange(page: number) {
  sourcePage.value = page;
  await refreshSourceFiles();
}

async function writeSourceToWiki(sourceFile: SourceFile) {
  wikiWritingFileUid.value = sourceFile.fileUid;
  try {
    const run = await createWikiIngestRun(sourceFile.fileUid);
    ElMessage.success(`Wiki 写入完成：${run.writeStatusLabel || run.statusLabel}`);
    await refreshSourceFiles();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Wiki 写入失败');
  } finally {
    wikiWritingFileUid.value = '';
  }
}

onMounted(refreshJobs);
</script>

<template>
  <div class="mvp0-stack">
    <el-card shadow="never">
      <template #header>
        <div class="section-title">
          <div class="card-title">
            <el-icon><Files /></el-icon>
            收纳任务
          </div>
          <div class="toolbar-inline">
            <el-select
              v-model="statusFilter"
              class="status-filter"
              clearable
              placeholder="全部状态"
              @change="refreshJobs"
            >
              <el-option
                v-for="item in importJobStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-button :loading="jobLoading" @click="refreshJobs">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        v-loading="jobLoading"
        :data="jobs"
        border
        highlight-current-row
        empty-text="暂无收纳任务"
        @row-click="selectJob"
      >
        <el-table-column prop="jobUid" label="任务编号" min-width="190" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="120">
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
        <el-table-column label="来源路径" min-width="240" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.inputPath }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="180" show-overflow-tooltip />
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-if="jobTotal > jobPageSize"
          background
          layout="prev, pager, next"
          :current-page="jobPage"
          :page-size="jobPageSize"
          :total="jobTotal"
          @current-change="handleJobPageChange"
        />
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="section-title">
          <div class="card-title">资料仓库文件</div>
          <el-button
            :disabled="!selectedJobUid"
            :loading="detailLoading || sourceLoading"
            @click="refreshSelectedJob"
          >
            <el-icon><Refresh /></el-icon>
            刷新文件
          </el-button>
        </div>
      </template>

      <div v-if="selectedJobDetail" class="job-detail-strip">
        <span class="detail-item">{{ selectedJobDetail.jobUid }}</span>
        <el-tag
          :class="getImportJobStatusDisplay(selectedJobDetail.statusCode || selectedJobDetail.status).className"
          :type="getImportJobStatusDisplay(selectedJobDetail.statusCode || selectedJobDetail.status).tagType"
          effect="plain"
        >
          {{ getImportJobStatusDisplay(selectedJobDetail.statusCode || selectedJobDetail.status).label }}
        </el-tag>
        <span class="detail-item">总数 {{ selectedJobDetail.totalCount }}</span>
        <span class="detail-item">成功 {{ selectedJobDetail.successCount }}</span>
        <span class="detail-item">跳过 {{ selectedJobDetail.skippedCount }}</span>
        <span class="detail-item">失败 {{ selectedJobDetail.failedCount }}</span>
        <span v-if="selectedJobDetail.errorMessage" class="detail-error">
          {{ selectedJobDetail.errorMessage }}
        </span>
      </div>

      <el-table
        v-loading="sourceLoading"
        :data="sourceFiles"
        border
        empty-text="选择收纳任务后显示文件"
      >
        <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="fileExt" label="类型" width="90" show-overflow-tooltip />
        <el-table-column label="大小" width="110" align="right">
          <template #default="scope">
            {{ formatBytes(scope.row.fileSizeBytes || scope.row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="收纳状态" width="130" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.collectStatusLabel || scope.row.organizeStatus }}
          </template>
        </el-table-column>
        <el-table-column label="抽取状态" width="130" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.extractStatusLabel || scope.row.parseStatus }}
          </template>
        </el-table-column>
        <el-table-column prop="contentHash" label="hash" min-width="220" show-overflow-tooltip />
        <el-table-column label="收纳路径" min-width="280" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.rawSourceRelativePath || scope.row.managedPath }}
          </template>
        </el-table-column>
        <el-table-column label="Wiki" width="120" fixed="right">
          <template #default="scope">
            <el-button
              :loading="wikiWritingFileUid === scope.row.fileUid"
              size="small"
              @click.stop="writeSourceToWiki(scope.row)"
            >
              写入 Wiki
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-if="sourceTotal > sourcePageSize"
          background
          layout="prev, pager, next"
          :current-page="sourcePage"
          :page-size="sourcePageSize"
          :total="sourceTotal"
          @current-change="handleSourcePageChange"
        />
      </div>
    </el-card>
  </div>
</template>
