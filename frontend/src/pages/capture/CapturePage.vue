<script setup lang="ts">
import { FolderAdd, Refresh, UploadFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { createLocalImportJob, listImportJobs } from '../../api/import-jobs';
import { uploadSources } from '../../api/upload-sources';
import {
  getImportJobStatusDisplay,
  importJobStatusOptions
} from '../../features/status/statusDisplay';
import type { CreateLocalImportJobRequest, ImportJob } from '../../types/importJobs';

const importForm = reactive<CreateLocalImportJobRequest>({
  inputPath: '',
  rawSourcesRoot: '',
  recursive: true,
  organizeMode: 'copy',
  maxCopyFileSizeMb: 100
});

const statusFilter = ref('');
const jobs = ref<ImportJob[]>([]);
const loading = ref(false);
const creating = ref(false);
const uploadInput = ref<HTMLInputElement | null>(null);
const selectedUploadFiles = ref<File[]>([]);
const uploading = ref(false);

function optionalText(value: string | undefined): string | undefined {
  const trimmed = value?.trim() || '';
  return trimmed ? trimmed : undefined;
}

async function refreshJobs() {
  loading.value = true;
  try {
    const result = await listImportJobs({
      statusCode: statusFilter.value || undefined,
      page: 1,
      pageSize: 8
    });
    jobs.value = result.items;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '收纳任务加载失败');
  } finally {
    loading.value = false;
  }
}

async function createJob() {
  const inputPath = importForm.inputPath.trim();
  if (!inputPath) {
    ElMessage.warning('请输入需要收纳的本地路径');
    return;
  }

  creating.value = true;
  try {
    const job = await createLocalImportJob({
      inputPath,
      rawSourcesRoot: optionalText(importForm.rawSourcesRoot),
      recursive: importForm.recursive,
      organizeMode: 'copy',
      maxCopyFileSizeMb: importForm.maxCopyFileSizeMb
    });
    ElMessage.success(`收纳任务已创建：${job.jobUid}`);
    statusFilter.value = '';
    await refreshJobs();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '收纳任务创建失败');
  } finally {
    creating.value = false;
  }
}

function chooseUploadFiles() {
  uploadInput.value?.click();
}

function handleUploadFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  selectedUploadFiles.value = Array.from(target.files ?? []);
}

function handleUploadDrop(event: DragEvent) {
  selectedUploadFiles.value = Array.from(event.dataTransfer?.files ?? []);
  if (uploadInput.value) {
    uploadInput.value.value = '';
  }
}

function clearUploadFiles() {
  selectedUploadFiles.value = [];
  if (uploadInput.value) {
    uploadInput.value.value = '';
  }
}

async function uploadSelectedFiles() {
  if (selectedUploadFiles.value.length === 0) {
    ElMessage.warning('请先选择需要上传的文件');
    return;
  }

  uploading.value = true;
  try {
    const result = await uploadSources({ files: selectedUploadFiles.value });
    ElMessage.success(`上传收纳已完成：${result.uploadedCount} 个文件`);
    clearUploadFiles();
    statusFilter.value = '';
    await refreshJobs();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '文件上传失败');
  } finally {
    uploading.value = false;
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
            <el-icon><FolderAdd /></el-icon>
            本地路径收纳
          </div>
          <el-tag effect="plain">主流程</el-tag>
        </div>
      </template>

      <el-form class="import-form" label-position="top" @submit.prevent>
        <el-form-item label="来源路径">
          <el-input
            v-model="importForm.inputPath"
            clearable
            placeholder="例如 E:\\资料\\待读"
          />
        </el-form-item>

        <el-form-item label="最大文件 MB">
          <el-input-number
            v-model="importForm.maxCopyFileSizeMb"
            :min="1"
            :max="2048"
            controls-position="right"
          />
        </el-form-item>

        <el-form-item label="递归扫描">
          <el-switch v-model="importForm.recursive" active-text="开启" inactive-text="关闭" />
        </el-form-item>

        <el-form-item class="wide-field" label="Raw Sources 根目录">
          <el-input
            v-model="importForm.rawSourcesRoot"
            clearable
            placeholder="留空使用系统默认收纳目录"
          />
        </el-form-item>

        <div class="form-actions">
          <p class="form-hint">Raw Sources 是不可变事实源，后续 Wiki 页面只引用和整理它。</p>
          <el-button :loading="creating" type="primary" @click="createJob">
            <el-icon><FolderAdd /></el-icon>
            创建收纳任务
          </el-button>
        </div>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="section-title">
          <div class="card-title">
            <el-icon><UploadFilled /></el-icon>
            浏览器上传
          </div>
          <el-tag effect="plain">主流程</el-tag>
        </div>
      </template>

      <div class="upload-panel">
        <div
          class="upload-dropzone"
          role="button"
          tabindex="0"
          @click="chooseUploadFiles"
          @keydown.enter.prevent="chooseUploadFiles"
          @keydown.space.prevent="chooseUploadFiles"
          @drop.prevent="handleUploadDrop"
          @dragover.prevent
        >
          <input
            ref="uploadInput"
            class="native-file-input"
            multiple
            type="file"
            @change="handleUploadFileChange"
          />
          <el-icon class="upload-dropzone-icon"><UploadFilled /></el-icon>
          <strong>选择或拖入文件</strong>
          <span>文件会直接进入 Raw Sources，并登记到资料箱。</span>
        </div>

        <div v-if="selectedUploadFiles.length > 0" class="upload-file-list">
          <span
            v-for="(file, index) in selectedUploadFiles"
            :key="`${file.name}-${file.size}-${index}`"
          >
            {{ file.name }}
          </span>
        </div>

        <div class="upload-actions">
          <p class="form-hint">已选择 {{ selectedUploadFiles.length }} 个文件。</p>
          <el-button @click="chooseUploadFiles">重新选择</el-button>
          <el-button :disabled="selectedUploadFiles.length === 0" @click="clearUploadFiles">
            清空
          </el-button>
          <el-button
            :disabled="selectedUploadFiles.length === 0"
            :loading="uploading"
            type="primary"
            @click="uploadSelectedFiles"
          >
            <el-icon><UploadFilled /></el-icon>
            上传收纳
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="section-title">
          <div class="card-title">最近收纳任务</div>
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
            <el-button :loading="loading" @click="refreshJobs">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="jobs" border empty-text="暂无收纳任务">
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
        <el-table-column prop="totalCount" label="总数" width="90" align="right" />
        <el-table-column prop="successCount" label="成功" width="90" align="right" />
        <el-table-column prop="skippedCount" label="跳过" width="90" align="right" />
        <el-table-column prop="failedCount" label="失败" width="90" align="right" />
        <el-table-column label="来源路径" min-width="240" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.inputPathMasked || scope.row.inputPath }}
          </template>
        </el-table-column>
        <el-table-column prop="rawSourcesRoot" label="收纳目录" min-width="240" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>
