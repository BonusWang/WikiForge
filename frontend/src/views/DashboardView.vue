<script setup lang="ts">
import {
  Connection,
  Cpu,
  Document,
  EditPen,
  Files,
  FolderAdd,
  Link,
  Refresh,
  SetUp,
  View
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import {
  createLocalImportJob,
  getImportJob,
  listImportJobs,
  listSourceFiles
} from '../api/import-jobs';
import {
  generateSourceNoteDraft,
  getObsidianStatus,
  getSourceFileObsidianNote,
  initializeObsidianVault,
  previewObsidianNote,
  writeSourceNote
} from '../api/obsidian';
import { createAiReviewRun, listReviewItems } from '../api/review';
import { fetchBackendHealth, type BackendHealth } from '../services/http';
import { useAppStore } from '../stores/app';
import type {
  CreateLocalImportJobRequest,
  ImportJob,
  ImportJobDetail,
  ImportJobStatus,
  SourceFile
} from '../types/importJobs';
import type {
  ObsidianNote,
  ObsidianNotePreview,
  ObsidianVaultStatus,
  SourceNoteDraft
} from '../types/obsidianNotes';
import type { ReviewItem } from '../types/review';

const appStore = useAppStore();
const backendHealth = ref<BackendHealth | null>(null);
const healthLoading = ref(false);
const errorMessage = ref('');

const importForm = reactive<CreateLocalImportJobRequest>({
  inputPath: '',
  rawSourcesRoot: '',
  recursive: true,
  organizeMode: 'copy',
  maxCopyFileSizeMb: 100
});

const statusOptions: ImportJobStatus[] = [
  'pending',
  'running',
  'completed',
  'failed',
  'cancelled'
];

const statusLabels: Record<ImportJobStatus, string> = {
  pending: 'Pending',
  running: 'Running',
  completed: 'Completed',
  failed: 'Failed',
  cancelled: 'Cancelled'
};

const jobStatusFilter = ref<ImportJobStatus | ''>('');
const jobs = ref<ImportJob[]>([]);
const jobPage = ref(1);
const jobPageSize = ref(20);
const jobTotal = ref(0);
const jobListLoading = ref(false);
const createLoading = ref(false);

const selectedJobUid = ref('');
const selectedJobDetail = ref<ImportJobDetail | null>(null);
const detailLoading = ref(false);
const sourceFiles = ref<SourceFile[]>([]);
const sourceFilePage = ref(1);
const sourceFilePageSize = ref(50);
const sourceFileTotal = ref(0);
const sourceFilesLoading = ref(false);
const vaultInitializing = ref(false);
const vaultStatus = ref<ObsidianVaultStatus | null>(null);
const vaultStatusLoading = ref(false);
const selectedSourceFile = ref<SourceFile | null>(null);
const sourceNoteDrawerVisible = ref(false);
const sourceNoteLoadingFileUid = ref('');
const sourceNoteDraft = ref<SourceNoteDraft | null>(null);
const writtenNote = ref<ObsidianNote | null>(null);
const notePreview = ref<ObsidianNotePreview | null>(null);
const noteMarkdown = ref('');
const sourceNoteMode = ref<'draft' | 'existing'>('draft');
const writeNoteLoading = ref(false);
const previewNoteLoading = ref(false);
const reviewItems = ref<ReviewItem[]>([]);
const reviewPage = ref(1);
const reviewPageSize = ref(20);
const reviewTotal = ref(0);
const reviewItemsLoading = ref(false);
const aiReviewLoadingFileUid = ref('');
const reviewDrawerVisible = ref(false);
const selectedReviewItem = ref<ReviewItem | null>(null);

const selectedJobStatus = computed(() => {
  return selectedJobDetail.value?.status
    ? statusLabels[selectedJobDetail.value.status]
    : 'Not selected';
});

const vaultStatusLabel = computed(() => {
  if (!vaultStatus.value) {
    return 'UNKNOWN';
  }
  if (vaultStatus.value.exists && vaultStatus.value.writable) {
    return 'READY';
  }
  if (vaultStatus.value.exists) {
    return 'READ ONLY';
  }
  return 'MISSING';
});

const selectedReviewSuggested = computed(() => {
  return prettyJson(selectedReviewItem.value?.suggestedChanges || '');
});

async function refreshHealth() {
  healthLoading.value = true;
  errorMessage.value = '';
  try {
    backendHealth.value = await fetchBackendHealth();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '无法连接后端服务';
  } finally {
    healthLoading.value = false;
  }
}

async function refreshJobs() {
  jobListLoading.value = true;
  try {
    const result = await listImportJobs({
      status: jobStatusFilter.value || undefined,
      page: jobPage.value,
      pageSize: jobPageSize.value
    });
    jobs.value = result.items;
    jobTotal.value = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导入任务列表加载失败');
  } finally {
    jobListLoading.value = false;
  }
}

async function createJob() {
  const inputPath = importForm.inputPath.trim();
  const rawSourcesRoot = importForm.rawSourcesRoot.trim();

  if (!inputPath || !rawSourcesRoot) {
    ElMessage.warning('inputPath 和 rawSourcesRoot 必填');
    return;
  }

  createLoading.value = true;
  try {
    const job = await createLocalImportJob({
      inputPath,
      rawSourcesRoot,
      recursive: importForm.recursive,
      organizeMode: 'copy',
      maxCopyFileSizeMb: importForm.maxCopyFileSizeMb
    });
    ElMessage.success(`任务已创建：${job.jobUid}`);
    jobStatusFilter.value = '';
    jobPage.value = 1;
    await refreshJobs();
    await selectJob(job);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导入任务创建失败');
  } finally {
    createLoading.value = false;
  }
}

async function selectJob(job: ImportJob) {
  selectedJobUid.value = job.jobUid;
  sourceFilePage.value = 1;
  await Promise.all([refreshJobDetail(), refreshSourceFiles()]);
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
    ElMessage.error(error instanceof Error ? error.message : '导入任务详情加载失败');
  } finally {
    detailLoading.value = false;
  }
}

async function refreshSourceFiles() {
  if (!selectedJobUid.value) {
    sourceFiles.value = [];
    sourceFileTotal.value = 0;
    return;
  }

  sourceFilesLoading.value = true;
  try {
    const result = await listSourceFiles({
      jobUid: selectedJobUid.value,
      page: sourceFilePage.value,
      pageSize: sourceFilePageSize.value
    });
    sourceFiles.value = result.items;
    sourceFileTotal.value = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Source files 加载失败');
  } finally {
    sourceFilesLoading.value = false;
  }
}

async function refreshSelectedJob() {
  await Promise.all([refreshJobDetail(), refreshSourceFiles()]);
}

async function refreshVaultStatus() {
  vaultStatusLoading.value = true;
  try {
    vaultStatus.value = await getObsidianStatus();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Obsidian Vault 状态加载失败');
  } finally {
    vaultStatusLoading.value = false;
  }
}

async function refreshReviewItems() {
  reviewItemsLoading.value = true;
  try {
    const result = await listReviewItems({
      status: 'pending',
      page: reviewPage.value,
      pageSize: reviewPageSize.value
    });
    reviewItems.value = result.items;
    reviewTotal.value = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '审核队列加载失败');
  } finally {
    reviewItemsLoading.value = false;
  }
}

async function initializeVault() {
  vaultInitializing.value = true;
  try {
    const result = await initializeObsidianVault();
    ElMessage.success(`Vault 已初始化：${result.vaultName}`);
    await refreshVaultStatus();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Obsidian Vault 初始化失败');
  } finally {
    vaultInitializing.value = false;
  }
}

async function runAiReview(sourceFile: SourceFile) {
  aiReviewLoadingFileUid.value = sourceFile.fileUid;
  try {
    const run = await createAiReviewRun(sourceFile.fileUid, {
      providerName: 'minimax',
      configSource: 'env'
    });
    ElMessage.success(`AI 整理已生成：${run.reviewItemUid}`);
    reviewPage.value = 1;
    await refreshReviewItems();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'AI 整理生成失败');
  } finally {
    aiReviewLoadingFileUid.value = '';
  }
}

function resetSourceNoteState(sourceFile: SourceFile, mode: 'draft' | 'existing') {
  selectedSourceFile.value = sourceFile;
  sourceNoteDrawerVisible.value = true;
  sourceNoteMode.value = mode;
  sourceNoteDraft.value = null;
  writtenNote.value = null;
  notePreview.value = null;
  noteMarkdown.value = '';
}

async function openSourceNote(sourceFile: SourceFile) {
  if (sourceFile.obsidianNoteUid) {
    await openExistingSourceNote(sourceFile);
    return;
  }
  await openSourceNoteDraft(sourceFile);
}

async function openExistingSourceNote(sourceFile: SourceFile) {
  resetSourceNoteState(sourceFile, 'existing');
  sourceNoteLoadingFileUid.value = sourceFile.fileUid;

  try {
    const note = await getSourceFileObsidianNote(sourceFile.fileUid);
    if (!note) {
      ElMessage.info('该文件尚未写入 Source Note，已切换为草案模式');
      await openSourceNoteDraft(sourceFile);
      return;
    }
    writtenNote.value = note;
    await loadNotePreview(note.noteUid);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '已有 Source Note 加载失败');
  } finally {
    sourceNoteLoadingFileUid.value = '';
  }
}

async function openSourceNoteDraft(sourceFile: SourceFile) {
  resetSourceNoteState(sourceFile, 'draft');
  sourceNoteLoadingFileUid.value = sourceFile.fileUid;

  try {
    const draft = await generateSourceNoteDraft(sourceFile.fileUid);
    sourceNoteDraft.value = draft;
    noteMarkdown.value = draft.markdown;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Source Note 草案生成失败');
  } finally {
    sourceNoteLoadingFileUid.value = '';
  }
}

async function persistSourceNote() {
  if (!selectedSourceFile.value) {
    return;
  }

  writeNoteLoading.value = true;
  try {
    const note = await writeSourceNote(selectedSourceFile.value.fileUid, {
      markdown: noteMarkdown.value
    });
    writtenNote.value = note;
    sourceNoteMode.value = 'existing';
    ElMessage.success('Source Note 已写入 Obsidian Vault');
    await loadNotePreview(note.noteUid);
    await Promise.all([refreshSourceFiles(), refreshVaultStatus()]);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Source Note 写入失败');
  } finally {
    writeNoteLoading.value = false;
  }
}

async function loadNotePreview(noteUid?: string) {
  const targetNoteUid = noteUid || writtenNote.value?.noteUid;
  if (!targetNoteUid) {
    return;
  }

  previewNoteLoading.value = true;
  try {
    const preview = await previewObsidianNote(targetNoteUid);
    notePreview.value = preview;
    noteMarkdown.value = preview.markdown;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Obsidian 文件预览失败');
  } finally {
    previewNoteLoading.value = false;
  }
}

function handleJobPageChange(page: number) {
  jobPage.value = page;
  void refreshJobs();
}

function handleSourceFilePageChange(page: number) {
  sourceFilePage.value = page;
  void refreshSourceFiles();
}

function handleReviewPageChange(page: number) {
  reviewPage.value = page;
  void refreshReviewItems();
}

function handleStatusFilterChange() {
  jobPage.value = 1;
  void refreshJobs();
}

function openReviewItem(item: ReviewItem) {
  selectedReviewItem.value = item;
  reviewDrawerVisible.value = true;
}

function statusTagType(status: ImportJobStatus) {
  if (status === 'completed') {
    return 'success';
  }
  if (status === 'failed') {
    return 'danger';
  }
  if (status === 'running') {
    return 'warning';
  }
  if (status === 'cancelled') {
    return 'info';
  }
  return 'primary';
}

function formatBytes(value: number) {
  if (!Number.isFinite(value)) {
    return '-';
  }

  const units = ['B', 'KB', 'MB', 'GB'];
  let size = value;
  let unitIndex = 0;

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex += 1;
  }

  return `${size.toFixed(unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
}

function prettyJson(value: string) {
  if (!value.trim()) {
    return '';
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

onMounted(() => {
  void refreshHealth();
  void refreshJobs();
  void refreshVaultStatus();
  void refreshReviewItems();
});
</script>

<template>
  <main class="shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">{{ appStore.stage }}</p>
        <h1>{{ appStore.appName }}</h1>
      </div>
      <div class="topbar-actions">
        <el-button :loading="vaultInitializing" @click="initializeVault">
          <el-icon><FolderAdd /></el-icon>
          初始化 Vault
        </el-button>
        <el-button :loading="healthLoading" type="primary" @click="refreshHealth">
          <el-icon><Refresh /></el-icon>
          刷新状态
        </el-button>
      </div>
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
        <p class="metric">MVP 2.1</p>
        <p class="muted">可用性加固</p>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="card-title">
            <el-icon><Cpu /></el-icon>
            导入任务
          </div>
        </template>
        <p class="metric">{{ jobTotal }}</p>
        <p class="muted">当前筛选结果</p>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="card-title">
            <el-icon><Files /></el-icon>
            Source Files
          </div>
        </template>
        <p class="metric">{{ sourceFileTotal }}</p>
        <p class="muted">{{ selectedJobStatus }}</p>
      </el-card>

      <el-card shadow="never" v-loading="vaultStatusLoading">
        <template #header>
          <div class="section-title compact">
            <div class="card-title">
              <el-icon><FolderAdd /></el-icon>
              Obsidian Vault
            </div>
            <el-button link type="primary" @click="refreshVaultStatus">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>
        </template>
        <p class="metric" :class="{ ok: vaultStatus?.exists && vaultStatus?.writable }">
          {{ vaultStatusLabel }}
        </p>
        <p class="muted truncate" :title="vaultStatus?.vaultPath || ''">
          {{ vaultStatus?.vaultPath || '等待检测' }}
        </p>
        <div class="vault-status-tags">
          <el-tag size="small" :type="vaultStatus?.sourceNoteDirectoryExists ? 'success' : 'info'" effect="plain">
            Sources
          </el-tag>
          <el-tag v-if="vaultStatus?.lastNoteUid" size="small" effect="plain">
            {{ vaultStatus.lastNoteUid }}
          </el-tag>
        </div>
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

    <section class="dashboard-stack">
      <el-card shadow="never">
        <template #header>
          <div class="section-title">
            <div class="card-title">
              <el-icon><FolderAdd /></el-icon>
              本地导入
            </div>
            <el-button :loading="jobListLoading" @click="refreshJobs">
              <el-icon><Refresh /></el-icon>
              刷新任务
            </el-button>
          </div>
        </template>

        <el-form class="import-form" label-position="top">
          <el-form-item label="inputPath" required>
            <el-input
              v-model="importForm.inputPath"
              clearable
              placeholder="E:/example/messy-sources"
            />
          </el-form-item>

          <el-form-item label="rawSourcesRoot" required>
            <el-input
              v-model="importForm.rawSourcesRoot"
              clearable
              placeholder="E:/WikiForge_RawSources"
            />
          </el-form-item>

          <el-form-item label="recursive">
            <el-switch v-model="importForm.recursive" />
          </el-form-item>

          <el-form-item label="最大复制文件大小(MB)">
            <el-input-number
              v-model="importForm.maxCopyFileSizeMb"
              :min="1"
              :max="10240"
              controls-position="right"
            />
          </el-form-item>

          <div class="form-actions">
            <el-tag type="info" effect="plain">organizeMode: copy</el-tag>
            <el-button :loading="createLoading" type="primary" @click="createJob">
              创建任务
            </el-button>
          </div>
        </el-form>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="section-title">
            <div class="card-title">
              <el-icon><SetUp /></el-icon>
              导入任务列表
            </div>
            <el-select
              v-model="jobStatusFilter"
              class="status-filter"
              clearable
              placeholder="全部状态"
              @change="handleStatusFilterChange"
            >
              <el-option
                v-for="status in statusOptions"
                :key="status"
                :label="statusLabels[status]"
                :value="status"
              />
            </el-select>
          </div>
        </template>

        <el-table
          v-loading="jobListLoading"
          :data="jobs"
          border
          highlight-current-row
          empty-text="暂无导入任务"
          @row-click="selectJob"
        >
          <el-table-column prop="jobUid" label="jobUid" min-width="180" show-overflow-tooltip />
          <el-table-column prop="status" label="status" width="120">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row.status)">
                {{ statusLabels[scope.row.status as ImportJobStatus] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="totalCount" label="total" width="86" align="right" />
          <el-table-column prop="successCount" label="success" width="94" align="right" />
          <el-table-column prop="skippedCount" label="skipped" width="94" align="right" />
          <el-table-column prop="failedCount" label="failed" width="86" align="right" />
          <el-table-column prop="inputPath" label="inputPath" min-width="220" show-overflow-tooltip />
          <el-table-column prop="rawSourcesRoot" label="rawSourcesRoot" min-width="220" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="createdAt" min-width="180" show-overflow-tooltip />
          <el-table-column fixed="right" label="操作" width="110">
            <template #default="scope">
              <el-button link type="primary" @click.stop="selectJob(scope.row)">查看文件</el-button>
            </template>
          </el-table-column>
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
            <div class="card-title">
              <el-icon><Files /></el-icon>
              Source Files
            </div>
            <el-button
              :disabled="!selectedJobUid"
              :loading="sourceFilesLoading || detailLoading"
              @click="refreshSelectedJob"
            >
              <el-icon><Refresh /></el-icon>
              刷新文件
            </el-button>
          </div>
        </template>

        <div v-if="selectedJobDetail" class="job-detail-strip">
          <span class="detail-item">{{ selectedJobDetail.jobUid }}</span>
          <el-tag :type="statusTagType(selectedJobDetail.status)">
            {{ statusLabels[selectedJobDetail.status] }}
          </el-tag>
          <span class="detail-item">total {{ selectedJobDetail.totalCount }}</span>
          <span class="detail-item">success {{ selectedJobDetail.successCount }}</span>
          <span class="detail-item">skipped {{ selectedJobDetail.skippedCount }}</span>
          <span class="detail-item">failed {{ selectedJobDetail.failedCount }}</span>
          <span v-if="selectedJobDetail.errorMessage" class="detail-error">
            {{ selectedJobDetail.errorMessage }}
          </span>
        </div>

        <el-table
          v-loading="sourceFilesLoading"
          :data="sourceFiles"
          border
          empty-text="选择导入任务后显示 source files"
        >
          <el-table-column prop="fileName" label="fileName" min-width="180" show-overflow-tooltip />
          <el-table-column prop="fileExt" label="fileExt" width="100" show-overflow-tooltip />
          <el-table-column prop="originalPath" label="originalPath" min-width="260" show-overflow-tooltip />
          <el-table-column prop="managedPath" label="managedPath" min-width="260" show-overflow-tooltip />
          <el-table-column prop="fileSize" label="fileSize" width="120" align="right">
            <template #default="scope">
              {{ formatBytes(scope.row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column prop="contentHash" label="contentHash" min-width="220" show-overflow-tooltip />
          <el-table-column prop="organizeStatus" label="organizeStatus" width="150" show-overflow-tooltip />
          <el-table-column label="Obsidian" width="140">
            <template #default="scope">
              <el-tag
                :type="scope.row.obsidianNoteUid ? 'success' : 'info'"
                effect="plain"
              >
                {{ scope.row.obsidianNoteUid ? '已写入' : '未写入' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column fixed="right" label="操作" width="220">
            <template #default="scope">
              <div class="row-actions">
                <el-button
                  link
                  type="primary"
                  :loading="sourceNoteLoadingFileUid === scope.row.fileUid"
                  @click="openSourceNote(scope.row)"
                >
                  <el-icon><Document /></el-icon>
                  {{ scope.row.obsidianNoteUid ? '预览 Note' : '生成 Note' }}
                </el-button>
                <el-button
                  link
                  type="success"
                  :loading="aiReviewLoadingFileUid === scope.row.fileUid"
                  @click="runAiReview(scope.row)"
                >
                  <el-icon><Cpu /></el-icon>
                  AI 整理
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-row">
          <el-pagination
            v-if="sourceFileTotal > sourceFilePageSize"
            background
            layout="prev, pager, next"
            :current-page="sourceFilePage"
            :page-size="sourceFilePageSize"
            :total="sourceFileTotal"
            @current-change="handleSourceFilePageChange"
          />
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="section-title">
            <div class="card-title">
              <el-icon><Cpu /></el-icon>
              审核队列
            </div>
            <el-button :loading="reviewItemsLoading" @click="refreshReviewItems">
              <el-icon><Refresh /></el-icon>
              刷新审核
            </el-button>
          </div>
        </template>

        <el-table
          v-loading="reviewItemsLoading"
          :data="reviewItems"
          border
          empty-text="暂无待审核 AI 整理建议"
        >
          <el-table-column prop="reviewUid" label="reviewUid" min-width="180" show-overflow-tooltip />
          <el-table-column prop="sourceFileUid" label="sourceFileUid" min-width="160" show-overflow-tooltip />
          <el-table-column prop="reviewType" label="reviewType" width="130" show-overflow-tooltip />
          <el-table-column prop="status" label="status" width="120">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'pending' ? 'warning' : 'info'">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="reason" min-width="220" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="createdAt" min-width="180" show-overflow-tooltip />
          <el-table-column fixed="right" label="操作" width="120">
            <template #default="scope">
              <el-button link type="primary" @click="openReviewItem(scope.row)">
                <el-icon><View /></el-icon>
                查看草案
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-row">
          <el-pagination
            v-if="reviewTotal > reviewPageSize"
            background
            layout="prev, pager, next"
            :current-page="reviewPage"
            :page-size="reviewPageSize"
            :total="reviewTotal"
            @current-change="handleReviewPageChange"
          />
        </div>
      </el-card>
    </section>

    <el-drawer
      v-model="sourceNoteDrawerVisible"
      size="min(760px, 92vw)"
      :title="selectedSourceFile?.fileName || 'Source Note'"
    >
      <div class="source-note-panel">
        <div class="source-note-mode-row">
          <el-tag :type="sourceNoteMode === 'existing' ? 'success' : 'primary'" effect="plain">
            {{ sourceNoteMode === 'existing' ? '已有 Source Note' : 'Source Note 草案' }}
          </el-tag>
          <span v-if="selectedSourceFile?.obsidianVaultPath" class="muted truncate">
            {{ selectedSourceFile.obsidianVaultPath }}
          </span>
        </div>

        <div v-if="sourceNoteDraft" class="source-note-meta">
          <el-tag effect="plain">{{ sourceNoteDraft.vaultName }}</el-tag>
          <span>{{ sourceNoteDraft.vaultPath }}</span>
        </div>

        <el-alert
          v-if="writtenNote"
          class="note-alert"
          type="success"
          show-icon
          :closable="false"
          :title="`已写入：${writtenNote.vaultPath}`"
        />

        <div class="note-toolbar">
          <el-button
            v-if="sourceNoteMode === 'draft'"
            type="primary"
            :disabled="!sourceNoteDraft"
            :loading="writeNoteLoading"
            @click="persistSourceNote"
          >
            <el-icon><EditPen /></el-icon>
            写入 Vault
          </el-button>
          <el-button
            :disabled="!writtenNote"
            :loading="previewNoteLoading"
            @click="loadNotePreview()"
          >
            <el-icon><View /></el-icon>
            读取预览
          </el-button>
          <el-button
            v-if="writtenNote"
            tag="a"
            :href="writtenNote.obsidianUri"
          >
            <el-icon><Link /></el-icon>
            打开 Obsidian
          </el-button>
        </div>

        <el-input
          v-model="noteMarkdown"
          class="markdown-editor"
          type="textarea"
          :readonly="sourceNoteMode === 'existing'"
          :rows="22"
          resize="vertical"
        />

        <div v-if="notePreview" class="source-note-meta">
          <el-tag type="success" effect="plain">{{ notePreview.noteUid }}</el-tag>
          <span>{{ notePreview.obsidianUri }}</span>
        </div>
      </div>
    </el-drawer>

    <el-drawer
      v-model="reviewDrawerVisible"
      size="min(760px, 92vw)"
      :title="selectedReviewItem?.reviewUid || 'AI 审核草案'"
    >
      <div v-if="selectedReviewItem" class="review-panel">
        <div class="source-note-mode-row">
          <el-tag type="warning" effect="plain">{{ selectedReviewItem.status }}</el-tag>
          <span class="muted truncate">
            {{ selectedReviewItem.sourceFileUid || selectedReviewItem.sourceUid }}
          </span>
        </div>

        <el-alert
          class="note-alert"
          type="info"
          show-icon
          :closable="false"
          :title="selectedReviewItem.reason || '等待人工审核后再写入知识层'"
        />

        <div class="review-section">
          <div class="card-title">结构化建议</div>
          <el-input
            :model-value="selectedReviewSuggested"
            class="markdown-editor"
            type="textarea"
            readonly
            :rows="12"
            resize="vertical"
          />
        </div>

        <div class="review-section">
          <div class="card-title">Markdown 草案</div>
          <el-input
            :model-value="selectedReviewItem.markdownDraft || ''"
            class="markdown-editor"
            type="textarea"
            readonly
            :rows="14"
            resize="vertical"
          />
        </div>
      </div>
    </el-drawer>
  </main>
</template>
