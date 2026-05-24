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
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import {
  createLocalImportJob,
  getImportJob,
  listImportJobs,
  listSourceFiles
} from '../api/import-jobs';
import {
  createLinkSource,
  createPersonalRecord,
  getPersonalRecordSummary,
  listPersonalRecords,
  writePersonalRecordObsidianNote
} from '../api/lifeos';
import {
  createKnowledgeMaintenanceRun,
  listKnowledgeMaintenanceItems,
  listKnowledgeMaintenanceRuns,
  updateKnowledgeMaintenanceItemStatus
} from '../api/knowledge-maintenance';
import { listMcpCalls, listMcpTools } from '../api/mcp';
import {
  generateSourceNoteDraft,
  getObsidianStatus,
  getSourceFileObsidianNote,
  initializeObsidianVault,
  previewObsidianNote,
  writeSourceNote
} from '../api/obsidian';
import { approveReviewItem, createAiReviewRun, listReviewItems } from '../api/review';
import { createVectorExport, listVectorExports } from '../api/vector-exports';
import {
  approveWikiIntegration,
  createWikiCompileRun,
  createWikiPage,
  listWikiIntegrations,
  listWikiPages,
  rejectWikiIntegration
} from '../api/wiki';
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
  LinkSourceResponse,
  PersonalRecord,
  PersonalRecordSummaryResponse,
  PersonalRecordType,
  SensitivityLevel
} from '../types/lifeos';
import type {
  KnowledgeMaintenanceItem,
  KnowledgeMaintenanceRun
} from '../types/knowledgeMaintenance';
import type {
  ObsidianNote,
  ObsidianNotePreview,
  ObsidianVaultStatus,
  SourceNoteDraft
} from '../types/obsidianNotes';
import type { McpCallStatus, McpToolCallLog, McpToolDefinition } from '../types/mcp';
import type { CreateAiReviewRunRequest, ReviewItem } from '../types/review';
import type {
  VectorExportJob,
  VectorExportScope
} from '../types/vectorExports';
import type {
  WikiIntegration,
  WikiIntegrationStatus,
  WikiPage,
  WikiPageStatus,
  WikiPageType,
  WikiRiskLevel
} from '../types/wiki';

type ConsolePage = 'overview' | 'capture' | 'inbox' | 'wiki-compile' | 'review' | 'obsidian' | 'advanced';

const appStore = useAppStore();
const backendHealth = ref<BackendHealth | null>(null);
const healthLoading = ref(false);
const errorMessage = ref('');
const activePage = ref<ConsolePage>('overview');

const importForm = reactive<CreateLocalImportJobRequest>({
  inputPath: '',
  rawSourcesRoot: '',
  recursive: true,
  organizeMode: 'copy',
  maxCopyFileSizeMb: 100
});

const linkSourceForm = reactive({
  title: '',
  sourceUrl: '',
  sourcePlatform: '',
  rawContent: '',
  sourceType: 'link',
  processingIntent: 'organize_only'
});

const personalRecordForm = reactive({
  recordType: 'note' as PersonalRecordType,
  title: '',
  occurredAt: '',
  rawContent: '',
  sourceChannel: 'manual',
  sourceRef: '',
  structuredText: '',
  sensitivityLevel: 'medium' as SensitivityLevel
});

const maintenanceForm = reactive({
  staleDays: 7,
  limit: 1000
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

const navigationGroups: {
  module: string;
  items: { page: ConsolePage; functionName: string; label: string }[];
}[] = [
  {
    module: 'Workflow 主流程',
    items: [
      { page: 'overview', functionName: '运行态势', label: '总览' },
      { page: 'capture', functionName: '本地导入 + 链接资料', label: '收集入口' },
      { page: 'inbox', functionName: '解析状态、重复、缺正文、待分类', label: '待整理资料' },
      { page: 'wiki-compile', functionName: 'Topic / Project 更新建议', label: 'Wiki 编译' },
      { page: 'review', functionName: '人工确认后写入', label: '审核队列' },
      { page: 'obsidian', functionName: '已确认 Wiki 页面', label: 'Obsidian / Wiki 页面' }
    ]
  },
  {
    module: 'Advanced 系统能力',
    items: [{ page: 'advanced', functionName: 'MCP / Vector Export / Health / Orchestration', label: '高级能力' }]
  }
];

const pageMeta: Record<ConsolePage, { title: string; subtitle: string }> = {
  overview: {
    title: '总览 / Workflow Overview',
    subtitle: '跟踪从资料收集、待整理、Wiki 编译到 Obsidian 写入的主链路状态。'
  },
  capture: {
    title: '收集入口 / Capture',
    subtitle: '日常只需要提交本地来源路径或链接资料；高级归集仓库覆盖默认折叠。'
  },
  inbox: {
    title: '待整理资料 / Source Inbox',
    subtitle: '查看导入任务、解析状态、重复资料、缺正文和待分类 Source Files。'
  },
  'wiki-compile': {
    title: 'Wiki 编译 / Topic & Project Compile',
    subtitle: '把 Source File 编译为 Topic / Project Wiki 页的追加建议，并进入自动写入或审核队列。'
  },
  review: {
    title: '审核队列 / Review Queue',
    subtitle: '集中处理需要人工确认的 Wiki 更新和旧版 AI 整理草案。'
  },
  obsidian: {
    title: 'Obsidian / Wiki 页面',
    subtitle: 'Topic / Project Wiki 页由用户主控，AI 只追加托管区块或提出更新建议。'
  },
  advanced: {
    title: '高级能力 / System Capabilities',
    subtitle: 'MCP、Vector Export、Knowledge Health 和 Orchestration 放在系统能力区，不抢主流程。'
  }
};

const activePageMeta = computed(() => pageMeta[activePage.value]);

const personalRecordTypeOptions = [
  { label: '消费 expense', value: 'expense' },
  { label: '账单 bill', value: 'bill' },
  { label: '邮件 email', value: 'email' },
  { label: '人际 relationship', value: 'relationship' },
  { label: '事件 event', value: 'event' },
  { label: '笔记 note', value: 'note' }
];

const sensitivityOptions: SensitivityLevel[] = ['low', 'medium', 'high'];

const aiProviderForm = reactive<CreateAiReviewRunRequest>({
  providerName: import.meta.env.VITE_WIKIFORGE_AI_PROVIDER || 'minimax',
  providerType: 'openai_compatible',
  modelName: import.meta.env.VITE_WIKIFORGE_AI_MODEL || '',
  baseUrl: '',
  configSource: 'env'
});

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
const approvingReviewUid = ref('');
const mcpTools = ref<McpToolDefinition[]>([]);
const mcpCalls = ref<McpToolCallLog[]>([]);
const mcpToolsLoading = ref(false);
const mcpCallsLoading = ref(false);
const mcpCallPage = ref(1);
const mcpCallPageSize = ref(20);
const mcpCallTotal = ref(0);
const mcpCallToolFilter = ref('');
const mcpCallStatusFilter = ref<McpCallStatus | ''>('');
const mcpCallCallerTypeFilter = ref('');
const createdLinkSource = ref<LinkSourceResponse | null>(null);
const linkSourceCreating = ref(false);
const personalRecords = ref<PersonalRecord[]>([]);
const personalSummary = ref<PersonalRecordSummaryResponse | null>(null);
const personalRecordCreating = ref(false);
const personalRecordsLoading = ref(false);
const personalSummaryLoading = ref(false);
const personalRecordPage = ref(1);
const personalRecordPageSize = ref(20);
const personalRecordTotal = ref(0);
const personalRecordTypeFilter = ref<PersonalRecordType | ''>('');
const personalRecordStatusFilter = ref('');
const personalRecordSourceFilter = ref('');
const archivingRecordUid = ref('');
const maintenanceRuns = ref<KnowledgeMaintenanceRun[]>([]);
const maintenanceItems = ref<KnowledgeMaintenanceItem[]>([]);
const maintenanceRunCreating = ref(false);
const maintenanceRunsLoading = ref(false);
const maintenanceItemsLoading = ref(false);
const maintenanceRunPage = ref(1);
const maintenanceRunPageSize = ref(10);
const maintenanceRunTotal = ref(0);
const maintenanceItemPage = ref(1);
const maintenanceItemPageSize = ref(20);
const maintenanceItemTotal = ref(0);
const maintenanceRunStatusFilter = ref('');
const maintenanceRunUidFilter = ref('');
const maintenanceIssueTypeFilter = ref('');
const maintenanceItemStatusFilter = ref('open');
const maintenanceItemUpdatingUids = ref<Set<string>>(new Set());

const maintenanceIssueTypeOptions = [
  { label: '空正文 missing_source_content', value: 'missing_source_content' },
  { label: '重复正文 duplicate_source_content', value: 'duplicate_source_content' },
  { label: '未归档记录 unarchived_personal_record', value: 'unarchived_personal_record' },
  { label: '空向量导出 empty_vector_export', value: 'empty_vector_export' },
  { label: '长期 pending chunk stale_vector_chunk', value: 'stale_vector_chunk' }
];

const wikiPageForm = reactive({
  pageType: 'topic' as WikiPageType,
  title: '',
  slug: '',
  vaultPath: '',
  status: 'active' as WikiPageStatus
});

const wikiCompileForm = reactive({
  fileUid: '',
  targetPageUid: '',
  riskLevel: 'low' as WikiRiskLevel,
  confidenceScore: 0.86,
  changeSummary: '',
  proposedMarkdown: ''
});

const wikiPages = ref<WikiPage[]>([]);
const wikiPagePage = ref(1);
const wikiPagePageSize = ref(20);
const wikiPageTotal = ref(0);
const wikiPagesLoading = ref(false);
const wikiPageCreating = ref(false);
const wikiIntegrations = ref<WikiIntegration[]>([]);
const wikiIntegrationPage = ref(1);
const wikiIntegrationPageSize = ref(20);
const wikiIntegrationTotal = ref(0);
const wikiIntegrationStatusFilter = ref<WikiIntegrationStatus | ''>('pending_review');
const wikiIntegrationPageFilter = ref('');
const wikiIntegrationSourceFilter = ref('');
const wikiIntegrationsLoading = ref(false);
const wikiCompileLoading = ref(false);
const wikiDecisionLoadingUid = ref('');

const vectorExportForm = reactive({
  scope: 'sources' as VectorExportScope,
  targetCollection: 'wikiforge-default',
  maxChunkChars: 1200,
  limit: 1000
});

const vectorExports = ref<VectorExportJob[]>([]);
const vectorExportPage = ref(1);
const vectorExportPageSize = ref(10);
const vectorExportTotal = ref(0);
const vectorExportsLoading = ref(false);
const vectorExportCreating = ref(false);

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

  if (!inputPath) {
    ElMessage.warning('知识来源地址必填');
    return;
  }

  createLoading.value = true;
  try {
    const job = await createLocalImportJob({
      inputPath,
      rawSourcesRoot: optionalText(importForm.rawSourcesRoot),
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

async function refreshMcpTools() {
  mcpToolsLoading.value = true;
  try {
    const result = await listMcpTools();
    mcpTools.value = result.tools;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'MCP 工具清单加载失败');
  } finally {
    mcpToolsLoading.value = false;
  }
}

async function refreshMcpCalls() {
  mcpCallsLoading.value = true;
  try {
    const result = await listMcpCalls({
      toolName: optionalText(mcpCallToolFilter.value),
      status: optionalText(mcpCallStatusFilter.value),
      callerType: optionalText(mcpCallCallerTypeFilter.value),
      page: mcpCallPage.value,
      pageSize: mcpCallPageSize.value
    });
    mcpCalls.value = result.items;
    mcpCallTotal.value = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'MCP 调用日志加载失败');
  } finally {
    mcpCallsLoading.value = false;
  }
}

async function refreshMcpPreview() {
  await Promise.all([refreshMcpTools(), refreshMcpCalls()]);
}

async function createLifeLinkSource() {
  const title = linkSourceForm.title.trim();
  const sourceUrl = linkSourceForm.sourceUrl.trim();
  if (!title || !sourceUrl) {
    ElMessage.warning('链接标题和 URL 必填');
    return;
  }

  linkSourceCreating.value = true;
  try {
    const result = await createLinkSource({
      title,
      sourceUrl,
      sourcePlatform: optionalText(linkSourceForm.sourcePlatform),
      rawContent: optionalText(linkSourceForm.rawContent),
      sourceType: linkSourceForm.sourceType,
      processingIntent: linkSourceForm.processingIntent
    });
    createdLinkSource.value = result;
    linkSourceForm.title = '';
    linkSourceForm.sourceUrl = '';
    linkSourceForm.rawContent = '';
    ElMessage.success(`链接资料已收集：${result.sourceUid}`);
    await refreshJobs();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '链接资料收集失败');
  } finally {
    linkSourceCreating.value = false;
  }
}

async function refreshPersonalRecords() {
  personalRecordsLoading.value = true;
  try {
    const result = await listPersonalRecords({
      recordType: optionalText(personalRecordTypeFilter.value),
      status: optionalText(personalRecordStatusFilter.value),
      sourceChannel: optionalText(personalRecordSourceFilter.value),
      page: personalRecordPage.value,
      pageSize: personalRecordPageSize.value
    });
    personalRecords.value = result.items;
    personalRecordTotal.value = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '个人记录加载失败');
  } finally {
    personalRecordsLoading.value = false;
  }
}

async function refreshPersonalSummary() {
  personalSummaryLoading.value = true;
  try {
    personalSummary.value = await getPersonalRecordSummary('all');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '个人记录汇总加载失败');
  } finally {
    personalSummaryLoading.value = false;
  }
}

async function refreshLifeOs() {
  await Promise.all([refreshPersonalRecords(), refreshPersonalSummary()]);
}

async function refreshMaintenanceRuns() {
  maintenanceRunsLoading.value = true;
  try {
    const result = await listKnowledgeMaintenanceRuns({
      status: optionalText(maintenanceRunStatusFilter.value),
      page: maintenanceRunPage.value,
      pageSize: maintenanceRunPageSize.value
    });
    maintenanceRuns.value = result.items;
    maintenanceRunTotal.value = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '知识库体检运行记录加载失败');
  } finally {
    maintenanceRunsLoading.value = false;
  }
}

async function refreshMaintenanceItems() {
  maintenanceItemsLoading.value = true;
  try {
    const result = await listKnowledgeMaintenanceItems({
      runUid: optionalText(maintenanceRunUidFilter.value),
      issueType: optionalText(maintenanceIssueTypeFilter.value),
      status: optionalText(maintenanceItemStatusFilter.value),
      page: maintenanceItemPage.value,
      pageSize: maintenanceItemPageSize.value
    });
    maintenanceItems.value = result.items;
    maintenanceItemTotal.value = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '知识库体检问题列表加载失败');
  } finally {
    maintenanceItemsLoading.value = false;
  }
}

async function refreshKnowledgeMaintenance() {
  await Promise.all([refreshMaintenanceRuns(), refreshMaintenanceItems()]);
}

async function createMaintenanceRunNow() {
  maintenanceRunCreating.value = true;
  try {
    const result = await createKnowledgeMaintenanceRun({
      staleDays: maintenanceForm.staleDays,
      limit: maintenanceForm.limit
    });
    ElMessage.success(`知识库体检完成：${result.issueCount} issues`);
    maintenanceRunUidFilter.value = result.runUid;
    maintenanceRunPage.value = 1;
    maintenanceItemPage.value = 1;
    await refreshKnowledgeMaintenance();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '知识库体检运行失败');
  } finally {
    maintenanceRunCreating.value = false;
  }
}

async function refreshWikiPages() {
  wikiPagesLoading.value = true;
  try {
    const result = await listWikiPages({
      status: 'active',
      page: wikiPagePage.value,
      pageSize: wikiPagePageSize.value
    });
    wikiPages.value = result.items;
    wikiPageTotal.value = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Wiki 页面加载失败');
  } finally {
    wikiPagesLoading.value = false;
  }
}

async function refreshWikiIntegrations() {
  wikiIntegrationsLoading.value = true;
  try {
    const result = await listWikiIntegrations({
      status: wikiIntegrationStatusFilter.value || undefined,
      pageUid: optionalText(wikiIntegrationPageFilter.value),
      sourceUid: optionalText(wikiIntegrationSourceFilter.value),
      page: wikiIntegrationPage.value,
      pageSize: wikiIntegrationPageSize.value
    });
    wikiIntegrations.value = result.items;
    wikiIntegrationTotal.value = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Wiki 更新记录加载失败');
  } finally {
    wikiIntegrationsLoading.value = false;
  }
}

async function refreshWikiCompile() {
  await Promise.all([refreshWikiPages(), refreshWikiIntegrations()]);
}

async function refreshWikiReviewQueue() {
  wikiIntegrationStatusFilter.value = 'pending_review';
  wikiIntegrationPage.value = 1;
  await refreshWikiIntegrations();
}

async function createWikiPageFromForm() {
  const title = wikiPageForm.title.trim();
  const vaultPath = wikiPageForm.vaultPath.trim();
  if (!title || !vaultPath) {
    ElMessage.warning('Wiki 页面标题和 Vault 路径必填');
    return;
  }

  wikiPageCreating.value = true;
  try {
    const page = await createWikiPage({
      pageType: wikiPageForm.pageType,
      title,
      slug: optionalText(wikiPageForm.slug),
      vaultPath,
      status: wikiPageForm.status
    });
    ElMessage.success(`Wiki 页面已注册：${page.pageUid}`);
    wikiPageForm.title = '';
    wikiPageForm.slug = '';
    wikiPageForm.vaultPath = '';
    wikiPagePage.value = 1;
    await refreshWikiPages();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Wiki 页面注册失败');
  } finally {
    wikiPageCreating.value = false;
  }
}

function selectSourceFileForWikiCompile(sourceFile: SourceFile) {
  wikiCompileForm.fileUid = sourceFile.fileUid;
  activePage.value = 'wiki-compile';
}

async function runWikiCompile() {
  const fileUid = wikiCompileForm.fileUid.trim();
  if (!fileUid) {
    ElMessage.warning('请选择或填写 Source File UID');
    return;
  }

  wikiCompileLoading.value = true;
  try {
    const result = await createWikiCompileRun(fileUid, {
      targetPageUid: optionalText(wikiCompileForm.targetPageUid),
      riskLevel: wikiCompileForm.riskLevel,
      confidenceScore: wikiCompileForm.confidenceScore,
      changeSummary: optionalText(wikiCompileForm.changeSummary),
      proposedMarkdown: optionalText(wikiCompileForm.proposedMarkdown)
    });
    ElMessage.success(`Wiki 编译完成：${result.status}`);
    wikiIntegrationStatusFilter.value = result.status;
    wikiIntegrationPage.value = 1;
    await refreshWikiIntegrations();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Wiki 编译失败');
  } finally {
    wikiCompileLoading.value = false;
  }
}

async function approveWikiUpdate(integration: WikiIntegration) {
  wikiDecisionLoadingUid.value = integration.integrationUid;
  try {
    await approveWikiIntegration(integration.integrationUid);
    ElMessage.success('Wiki 更新已写入托管区块');
    await refreshWikiIntegrations();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Wiki 更新审核通过失败');
  } finally {
    wikiDecisionLoadingUid.value = '';
  }
}

async function rejectWikiUpdate(integration: WikiIntegration) {
  try {
    await ElMessageBox.confirm('拒绝后不会写入 Obsidian Wiki 页面。', '拒绝 Wiki 更新建议', {
      confirmButtonText: '拒绝',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return;
    }
  }

  wikiDecisionLoadingUid.value = integration.integrationUid;
  try {
    await rejectWikiIntegration(integration.integrationUid);
    ElMessage.success('Wiki 更新建议已拒绝');
    await refreshWikiIntegrations();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Wiki 更新拒绝失败');
  } finally {
    wikiDecisionLoadingUid.value = '';
  }
}

async function refreshVectorExports() {
  vectorExportsLoading.value = true;
  try {
    const result = await listVectorExports({
      page: vectorExportPage.value,
      pageSize: vectorExportPageSize.value
    });
    vectorExports.value = result.items;
    vectorExportTotal.value = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Vector Export 记录加载失败');
  } finally {
    vectorExportsLoading.value = false;
  }
}

async function createVectorExportJob() {
  vectorExportCreating.value = true;
  try {
    const result = await createVectorExport({
      scope: vectorExportForm.scope,
      targetCollection: optionalText(vectorExportForm.targetCollection),
      maxChunkChars: vectorExportForm.maxChunkChars,
      limit: vectorExportForm.limit
    });
    ElMessage.success(`Vector Export 已完成：${result.totalCount} chunks`);
    vectorExportPage.value = 1;
    await refreshVectorExports();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Vector Export 失败');
  } finally {
    vectorExportCreating.value = false;
  }
}

async function updateMaintenanceItemStatus(
  item: KnowledgeMaintenanceItem,
  status: 'open' | 'resolved' | 'ignored'
) {
  let resolutionNote = '';
  try {
    if (status === 'open') {
      await ElMessageBox.confirm('重新打开后该问题会回到 open 状态。', '重新打开体检问题', {
        confirmButtonText: '重新打开',
        cancelButtonText: '取消',
        type: 'warning'
      });
    } else {
      const actionLabel = status === 'resolved' ? '标记已解决' : '忽略问题';
      const result = await ElMessageBox.prompt('处理备注', actionLabel, {
        confirmButtonText: actionLabel,
        cancelButtonText: '取消',
        inputPlaceholder: '填写本次处理备注',
        inputValue: status === 'resolved' ? '已人工确认处理完成' : '已人工确认暂不处理'
      });
      resolutionNote = String(result.value || '').trim();
    }

    setMaintenanceItemUpdating(item.itemUid, true);
    await updateKnowledgeMaintenanceItemStatus(item.itemUid, {
      status,
      resolutionNote,
      resolvedBy: 'web-ui'
    });
    ElMessage.success(status === 'open' ? '体检问题已重新打开' : '体检问题状态已更新');
    await refreshMaintenanceItems();
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return;
    }
    ElMessage.error(error instanceof Error ? error.message : '维护问题状态更新失败');
  } finally {
    setMaintenanceItemUpdating(item.itemUid, false);
  }
}

async function createLifePersonalRecord() {
  const title = personalRecordForm.title.trim();
  const rawContent = personalRecordForm.rawContent.trim();
  if (!title || !rawContent) {
    ElMessage.warning('记录标题和原始内容必填');
    return;
  }
  const structured = parseStructuredText();
  if (structured === null) {
    return;
  }

  personalRecordCreating.value = true;
  try {
    const result = await createPersonalRecord({
      recordType: personalRecordForm.recordType,
      title,
      occurredAt: optionalText(personalRecordForm.occurredAt),
      rawContent,
      sourceChannel: optionalText(personalRecordForm.sourceChannel),
      sourceRef: optionalText(personalRecordForm.sourceRef),
      structured,
      sensitivityLevel: personalRecordForm.sensitivityLevel,
      createdBy: 'web-ui'
    });
    ElMessage.success(`个人记录已创建：${result.recordUid}`);
    personalRecordForm.title = '';
    personalRecordForm.rawContent = '';
    personalRecordForm.sourceRef = '';
    personalRecordForm.structuredText = '';
    personalRecordPage.value = 1;
    await refreshLifeOs();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '个人记录创建失败');
  } finally {
    personalRecordCreating.value = false;
  }
}

async function archivePersonalRecord(record: PersonalRecord) {
  archivingRecordUid.value = record.recordUid;
  try {
    const result = await writePersonalRecordObsidianNote(record.recordUid);
    ElMessage.success(`已写入 Obsidian：${result.vaultPath}`);
    await Promise.all([refreshPersonalRecords(), refreshPersonalSummary(), refreshVaultStatus()]);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '个人记录写入 Obsidian 失败');
  } finally {
    archivingRecordUid.value = '';
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
    const run = await createAiReviewRun(sourceFile.fileUid, aiReviewPayload());
    ElMessage.success(`AI 整理已生成：${run.reviewItemUid}`);
    reviewPage.value = 1;
    await refreshReviewItems();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'AI 整理生成失败');
  } finally {
    aiReviewLoadingFileUid.value = '';
  }
}

function aiReviewPayload(): CreateAiReviewRunRequest {
  return {
    providerName: optionalText(aiProviderForm.providerName),
    providerType: optionalText(aiProviderForm.providerType),
    modelName: optionalText(aiProviderForm.modelName),
    baseUrl: optionalText(aiProviderForm.baseUrl),
    configSource: optionalText(aiProviderForm.configSource) || 'env'
  };
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

function handleMcpCallPageChange(page: number) {
  mcpCallPage.value = page;
  void refreshMcpCalls();
}

function handleMcpFilterChange() {
  mcpCallPage.value = 1;
  void refreshMcpCalls();
}

function handlePersonalRecordPageChange(page: number) {
  personalRecordPage.value = page;
  void refreshPersonalRecords();
}

function handlePersonalRecordFilterChange() {
  personalRecordPage.value = 1;
  void refreshPersonalRecords();
}

function handleMaintenanceRunPageChange(page: number) {
  maintenanceRunPage.value = page;
  void refreshMaintenanceRuns();
}

function handleMaintenanceItemPageChange(page: number) {
  maintenanceItemPage.value = page;
  void refreshMaintenanceItems();
}

function handleMaintenanceRunFilterChange() {
  maintenanceRunPage.value = 1;
  void refreshMaintenanceRuns();
}

function handleMaintenanceItemFilterChange() {
  maintenanceItemPage.value = 1;
  void refreshMaintenanceItems();
}

function handleWikiPagePageChange(page: number) {
  wikiPagePage.value = page;
  void refreshWikiPages();
}

function handleWikiIntegrationPageChange(page: number) {
  wikiIntegrationPage.value = page;
  void refreshWikiIntegrations();
}

function handleWikiIntegrationFilterChange() {
  wikiIntegrationPage.value = 1;
  void refreshWikiIntegrations();
}

function handleVectorExportPageChange(page: number) {
  vectorExportPage.value = page;
  void refreshVectorExports();
}

function selectMaintenanceRun(run: KnowledgeMaintenanceRun) {
  maintenanceRunUidFilter.value = run.runUid;
  maintenanceItemPage.value = 1;
  void refreshMaintenanceItems();
}

function handleStatusFilterChange() {
  jobPage.value = 1;
  void refreshJobs();
}

function openReviewItem(item: ReviewItem) {
  selectedReviewItem.value = item;
  reviewDrawerVisible.value = true;
}

async function approveSelectedReviewItem() {
  const reviewItem = selectedReviewItem.value;
  if (!reviewItem || reviewItem.status !== 'pending') {
    return;
  }

  approvingReviewUid.value = reviewItem.reviewUid;
  try {
    const result = await approveReviewItem(reviewItem.reviewUid);
    ElMessage.success('审核已通过，并已写入 Obsidian');
    selectedReviewItem.value = {
      ...reviewItem,
      status: result.status
    };
    reviewDrawerVisible.value = false;
    openApprovedObsidianNote(result.obsidianNote);
    await Promise.all([refreshReviewItems(), refreshVaultStatus(), refreshSourceFiles()]);
    await loadNotePreview(result.obsidianNote.noteUid);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '审核通过失败');
  } finally {
    approvingReviewUid.value = '';
  }
}

function openApprovedObsidianNote(note: ObsidianNote) {
  selectedSourceFile.value = null;
  sourceNoteDrawerVisible.value = true;
  sourceNoteMode.value = 'existing';
  sourceNoteDraft.value = null;
  writtenNote.value = note;
  notePreview.value = null;
  noteMarkdown.value = '';
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

function mcpStatusTagType(status: McpCallStatus) {
  if (status === 'completed') {
    return 'success';
  }
  if (status === 'failed') {
    return 'danger';
  }
  return 'info';
}

function personalRecordTagType(status: string) {
  if (status === 'archived') {
    return 'success';
  }
  if (status === 'pending') {
    return 'warning';
  }
  if (status === 'failed') {
    return 'danger';
  }
  return 'info';
}

function maintenanceSeverityTagType(severity: string) {
  if (severity === 'high') {
    return 'danger';
  }
  if (severity === 'medium') {
    return 'warning';
  }
  return 'info';
}

function maintenanceStatusTagType(status: string) {
  if (status === 'completed' || status === 'resolved') {
    return 'success';
  }
  if (status === 'failed') {
    return 'danger';
  }
  if (status === 'open') {
    return 'warning';
  }
  return 'info';
}

function wikiIntegrationStatusTagType(status: string) {
  if (status === 'auto_applied' || status === 'approved') {
    return 'success';
  }
  if (status === 'pending_review') {
    return 'warning';
  }
  if (status === 'rejected') {
    return 'info';
  }
  return 'primary';
}

function statusBadgeClass(status: string) {
  return `status-badge status-badge--${status}`;
}

function setMaintenanceItemUpdating(itemUid: string, loading: boolean) {
  const next = new Set(maintenanceItemUpdatingUids.value);
  if (loading) {
    next.add(itemUid);
  } else {
    next.delete(itemUid);
  }
  maintenanceItemUpdatingUids.value = next;
}

function isMaintenanceItemUpdating(itemUid: string) {
  return maintenanceItemUpdatingUids.value.has(itemUid);
}

function recordTypeLabel(recordType: string) {
  return personalRecordTypeOptions.find((item) => item.value === recordType)?.label || recordType;
}

function summaryCount(key: string) {
  return personalSummary.value?.byType?.[key] || 0;
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

function parseStructuredText(): Record<string, unknown> | undefined | null {
  const text = personalRecordForm.structuredText.trim();
  if (!text) {
    return undefined;
  }
  try {
    return JSON.parse(text) as Record<string, unknown>;
  } catch {
    ElMessage.warning('结构化 JSON 格式不正确');
    return null;
  }
}

function optionalText(value?: string) {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
}

onMounted(() => {
  void refreshHealth();
  void refreshJobs();
  void refreshVaultStatus();
  void refreshReviewItems();
  void refreshMcpPreview();
  void refreshLifeOs();
  void refreshKnowledgeMaintenance();
  void refreshWikiCompile();
  void refreshVectorExports();
});
</script>

<template>
  <main class="app-shell">
    <aside class="sidebar-nav" aria-label="WikiForge navigation">
      <div class="sidebar-brand">
        <span class="brand-mark">WF</span>
        <div>
          <strong>{{ appStore.appName }}</strong>
          <span>Knowledge Forge</span>
        </div>
      </div>
      <div
        v-for="group in navigationGroups"
        :key="group.module"
        class="nav-group"
      >
        <p class="nav-group-title">{{ group.module }}</p>
        <button
          v-for="item in group.items"
          :key="item.page"
          class="nav-button"
          :class="{ 'is-active': activePage === item.page }"
          type="button"
          @click="activePage = item.page"
        >
          <span class="nav-dot" />
          <span>
            <strong>{{ item.label }}</strong>
            <small>{{ item.functionName }}</small>
          </span>
        </button>
      </div>
    </aside>

    <section class="shell workspace">
    <header class="topbar">
      <div>
        <p class="eyebrow">{{ appStore.stage }}</p>
        <h1>{{ activePageMeta.title }}</h1>
        <p class="page-subtitle">{{ activePageMeta.subtitle }}</p>
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

    <section v-if="activePage === 'overview'" class="status-grid">
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
        <p class="metric">V2</p>
        <p class="muted">收集、整理、体检与 Agent 接入</p>
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
      <el-card v-if="activePage === 'capture'" shadow="never">
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
          <el-form-item label="知识来源地址 sourcePath" required>
            <el-input
              v-model="importForm.inputPath"
              clearable
              placeholder="E:/个人知识体系/待整理资料"
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

          <el-collapse class="advanced-import-options">
            <el-collapse-item title="高级归集设置" name="raw-root">
              <el-form-item label="rawSourcesRoot 覆盖">
                <el-input
                  v-model="importForm.rawSourcesRoot"
                  clearable
                  placeholder="留空使用后端配置默认值"
                />
              </el-form-item>
            </el-collapse-item>
          </el-collapse>

          <div class="form-actions">
            <p class="form-hint">
              导入后默认复制归集到系统配置的 Raw Sources；只有需要校验高级覆盖时再展开填写。
            </p>
            <el-tag type="info" effect="plain">organizeMode: copy</el-tag>
            <el-button :loading="createLoading" type="primary" @click="createJob">
              创建任务
            </el-button>
          </div>
        </el-form>
      </el-card>

      <el-card v-if="activePage === 'advanced'" shadow="never">
        <template #header>
          <div class="section-title">
            <div class="card-title">
              <el-icon><SetUp /></el-icon>
              知识库体检 Knowledge Health
            </div>
            <el-button
              :loading="maintenanceRunsLoading || maintenanceItemsLoading"
              @click="refreshKnowledgeMaintenance"
            >
              <el-icon><Refresh /></el-icon>
              刷新巡检
            </el-button>
          </div>
        </template>

        <p class="section-helper">
          用于发现“收集了但没有整理好”的基础问题，例如空正文、重复正文、长期未归档个人记录。当前只做检查和标记，不会自动删除、移动或改写你的资料。
        </p>

        <el-form class="maintenance-form" label-position="top">
          <el-form-item label="staleDays">
            <el-input-number
              v-model="maintenanceForm.staleDays"
              :min="1"
              :max="365"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item label="limit">
            <el-input-number
              v-model="maintenanceForm.limit"
              :min="1"
              :max="10000"
              controls-position="right"
            />
          </el-form-item>
          <div class="form-actions">
            <el-tag effect="plain">manual check</el-tag>
            <el-button :loading="maintenanceRunCreating" type="primary" @click="createMaintenanceRunNow">
              开始体检
            </el-button>
          </div>
        </el-form>

        <div class="maintenance-board">
          <div class="maintenance-pane">
            <div class="maintenance-toolbar">
              <div class="card-title">运行记录</div>
              <el-select
                v-model="maintenanceRunStatusFilter"
                clearable
                placeholder="status"
                @change="handleMaintenanceRunFilterChange"
              >
                <el-option label="completed" value="completed" />
                <el-option label="failed" value="failed" />
              </el-select>
            </div>
            <el-table
              v-loading="maintenanceRunsLoading"
              :data="maintenanceRuns"
              border
              empty-text="暂无知识库体检运行"
            >
              <el-table-column prop="createdAt" label="createdAt" min-width="170" show-overflow-tooltip />
              <el-table-column prop="runUid" label="runUid" min-width="190" show-overflow-tooltip />
              <el-table-column prop="status" label="status" width="120">
                <template #default="scope">
                  <el-tag :type="maintenanceStatusTagType(scope.row.status)">
                    {{ scope.row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="issueCount" label="issues" width="90" align="right" />
              <el-table-column fixed="right" label="操作" width="100">
                <template #default="scope">
                  <el-button link type="primary" @click="selectMaintenanceRun(scope.row)">
                    查看
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination-row">
              <el-pagination
                v-if="maintenanceRunTotal > maintenanceRunPageSize"
                background
                layout="prev, pager, next"
                :current-page="maintenanceRunPage"
                :page-size="maintenanceRunPageSize"
                :total="maintenanceRunTotal"
                @current-change="handleMaintenanceRunPageChange"
              />
            </div>
          </div>

          <div class="maintenance-pane">
            <div class="maintenance-toolbar issue-toolbar">
              <el-input
                v-model="maintenanceRunUidFilter"
                clearable
                placeholder="runUid"
                @change="handleMaintenanceItemFilterChange"
                @clear="handleMaintenanceItemFilterChange"
              />
              <el-select
                v-model="maintenanceIssueTypeFilter"
                clearable
                placeholder="issueType"
                @change="handleMaintenanceItemFilterChange"
              >
                <el-option
                  v-for="item in maintenanceIssueTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <el-select
                v-model="maintenanceItemStatusFilter"
                clearable
                placeholder="status"
                @change="handleMaintenanceItemFilterChange"
              >
                <el-option label="open" value="open" />
                <el-option label="resolved" value="resolved" />
                <el-option label="ignored" value="ignored" />
              </el-select>
            </div>
            <el-table
              v-loading="maintenanceItemsLoading"
              :data="maintenanceItems"
              border
              empty-text="暂无知识库体检问题"
            >
              <el-table-column prop="createdAt" label="createdAt" min-width="170" show-overflow-tooltip />
              <el-table-column prop="issueType" label="issueType" min-width="210" show-overflow-tooltip />
              <el-table-column prop="severity" label="severity" width="110">
                <template #default="scope">
                  <el-tag :type="maintenanceSeverityTagType(scope.row.severity)">
                    {{ scope.row.severity }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="contentType" label="contentType" min-width="140" show-overflow-tooltip />
              <el-table-column prop="title" label="title" min-width="180" show-overflow-tooltip />
              <el-table-column prop="summary" label="summary" min-width="260" show-overflow-tooltip />
              <el-table-column prop="evidenceJson" label="evidence" min-width="220" show-overflow-tooltip />
              <el-table-column prop="status" label="status" width="110">
                <template #default="scope">
                  <el-tag :type="maintenanceStatusTagType(scope.row.status)">
                    {{ scope.row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="resolutionNote" label="resolution" min-width="180" show-overflow-tooltip />
              <el-table-column label="actions" width="230" fixed="right">
                <template #default="scope">
                  <template v-if="scope.row.status === 'open'">
                    <el-button
                      link
                      type="success"
                      :loading="isMaintenanceItemUpdating(scope.row.itemUid)"
                      @click="updateMaintenanceItemStatus(scope.row, 'resolved')"
                    >
                      已解决
                    </el-button>
                    <el-button
                      link
                      type="info"
                      :loading="isMaintenanceItemUpdating(scope.row.itemUid)"
                      @click="updateMaintenanceItemStatus(scope.row, 'ignored')"
                    >
                      忽略
                    </el-button>
                  </template>
                  <el-button
                    v-else
                    link
                    type="warning"
                    :loading="isMaintenanceItemUpdating(scope.row.itemUid)"
                    @click="updateMaintenanceItemStatus(scope.row, 'open')"
                  >
                    重新打开
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="pagination-row">
              <el-pagination
                v-if="maintenanceItemTotal > maintenanceItemPageSize"
                background
                layout="prev, pager, next"
                :current-page="maintenanceItemPage"
                :page-size="maintenanceItemPageSize"
                :total="maintenanceItemTotal"
                @current-change="handleMaintenanceItemPageChange"
              />
            </div>
          </div>
        </div>
      </el-card>

      <el-card v-if="activePage === 'capture'" shadow="never">
        <template #header>
          <div class="section-title">
            <div class="card-title">
              <el-icon><Link /></el-icon>
              LifeOS 收集
            </div>
            <el-button
              :loading="personalRecordsLoading || personalSummaryLoading"
              @click="refreshLifeOs"
            >
              <el-icon><Refresh /></el-icon>
              刷新 LifeOS
            </el-button>
          </div>
        </template>

        <div class="lifeos-grid">
          <div class="lifeos-form-pane">
            <div class="card-title">链接资料</div>
            <el-form class="lifeos-form" label-position="top">
              <el-form-item label="标题" required>
                <el-input v-model="linkSourceForm.title" clearable placeholder="飞书项目文档 / B站课程 / 知乎文章" />
              </el-form-item>
              <el-form-item label="URL" required>
                <el-input v-model="linkSourceForm.sourceUrl" clearable placeholder="https://..." />
              </el-form-item>
              <el-form-item label="平台">
                <el-select v-model="linkSourceForm.sourcePlatform" clearable placeholder="自动识别">
                  <el-option label="飞书 feishu" value="feishu" />
                  <el-option label="腾讯文档 tencent_doc" value="tencent_doc" />
                  <el-option label="微信 wechat" value="wechat" />
                  <el-option label="B站 bilibili" value="bilibili" />
                  <el-option label="知乎 zhihu" value="zhihu" />
                  <el-option label="网页 web" value="web" />
                </el-select>
              </el-form-item>
              <el-form-item label="正文 / 备注">
                <el-input
                  v-model="linkSourceForm.rawContent"
                  type="textarea"
                  :rows="4"
                  resize="vertical"
                  placeholder="可先贴正文或备注；留空则只收集链接，后续连接器读取"
                />
              </el-form-item>
              <div class="form-actions">
                <el-tag effect="plain">processing: {{ linkSourceForm.processingIntent }}</el-tag>
                <el-button :loading="linkSourceCreating" type="primary" @click="createLifeLinkSource">
                  收集链接
                </el-button>
              </div>
            </el-form>

            <el-alert
              v-if="createdLinkSource"
              class="note-alert"
              type="success"
              show-icon
              :closable="false"
              :title="`最近创建：${createdLinkSource.sourceUid} / ${createdLinkSource.fileUid}`"
            />
          </div>

          <div class="lifeos-form-pane">
            <div class="card-title">个人记录</div>
            <el-form class="lifeos-form" label-position="top">
              <div class="record-form-row">
                <el-form-item label="类型" required>
                  <el-select v-model="personalRecordForm.recordType">
                    <el-option
                      v-for="item in personalRecordTypeOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="敏感级别">
                  <el-select v-model="personalRecordForm.sensitivityLevel">
                    <el-option
                      v-for="level in sensitivityOptions"
                      :key="level"
                      :label="level"
                      :value="level"
                    />
                  </el-select>
                </el-form-item>
              </div>
              <el-form-item label="标题" required>
                <el-input v-model="personalRecordForm.title" clearable placeholder="咖啡消费 / 客户邮件 / 朋友近况" />
              </el-form-item>
              <div class="record-form-row">
                <el-form-item label="发生时间">
                  <el-date-picker
                    v-model="personalRecordForm.occurredAt"
                    type="datetime"
                    value-format="YYYY-MM-DDTHH:mm:ss"
                    placeholder="选择时间"
                  />
                </el-form-item>
                <el-form-item label="来源">
                  <el-input v-model="personalRecordForm.sourceChannel" clearable placeholder="manual / hermes / openclaw" />
                </el-form-item>
              </div>
              <el-form-item label="来源引用">
                <el-input v-model="personalRecordForm.sourceRef" clearable placeholder="邮件ID、聊天ID、账单编号或链接" />
              </el-form-item>
              <el-form-item label="原始内容" required>
                <el-input
                  v-model="personalRecordForm.rawContent"
                  type="textarea"
                  :rows="4"
                  resize="vertical"
                  placeholder="把账单、邮件摘要、人际关系备注或事件原文先记录下来"
                />
              </el-form-item>
              <el-form-item label="结构化 JSON">
                <el-input
                  v-model="personalRecordForm.structuredText"
                  type="textarea"
                  :rows="3"
                  resize="vertical"
                  placeholder='{"amount":18,"currency":"CNY"}'
                />
              </el-form-item>
              <div class="form-actions">
                <el-button :loading="personalRecordCreating" type="primary" @click="createLifePersonalRecord">
                  记录
                </el-button>
              </div>
            </el-form>
          </div>
        </div>

        <div class="lifeos-summary" v-loading="personalSummaryLoading">
          <div class="summary-pill">
            <span>总记录</span>
            <strong>{{ personalSummary?.total ?? 0 }}</strong>
          </div>
          <div
            v-for="item in personalRecordTypeOptions"
            :key="item.value"
            class="summary-pill"
          >
            <span>{{ item.value }}</span>
            <strong>{{ summaryCount(item.value) }}</strong>
          </div>
        </div>

        <div class="lifeos-record-toolbar">
          <el-select
            v-model="personalRecordTypeFilter"
            clearable
            placeholder="recordType"
            @change="handlePersonalRecordFilterChange"
          >
            <el-option
              v-for="item in personalRecordTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-select
            v-model="personalRecordStatusFilter"
            clearable
            placeholder="status"
            @change="handlePersonalRecordFilterChange"
          >
            <el-option label="pending" value="pending" />
            <el-option label="archived" value="archived" />
          </el-select>
          <el-input
            v-model="personalRecordSourceFilter"
            clearable
            placeholder="sourceChannel"
            @change="handlePersonalRecordFilterChange"
            @clear="handlePersonalRecordFilterChange"
          />
        </div>

        <el-table
          v-loading="personalRecordsLoading"
          :data="personalRecords"
          border
          empty-text="暂无个人记录"
        >
          <el-table-column prop="createdAt" label="createdAt" min-width="180" show-overflow-tooltip />
          <el-table-column prop="recordType" label="type" min-width="150" show-overflow-tooltip>
            <template #default="scope">
              {{ recordTypeLabel(scope.row.recordType) }}
            </template>
          </el-table-column>
          <el-table-column prop="title" label="title" min-width="180" show-overflow-tooltip />
          <el-table-column prop="sourceChannel" label="source" min-width="130" show-overflow-tooltip />
          <el-table-column prop="status" label="status" width="120">
            <template #default="scope">
              <el-tag :type="personalRecordTagType(scope.row.status)">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="rawContent" label="rawContent" min-width="220" show-overflow-tooltip />
          <el-table-column prop="obsidianVaultPath" label="Obsidian" min-width="220" show-overflow-tooltip />
          <el-table-column fixed="right" label="操作" width="210">
            <template #default="scope">
              <div class="row-actions">
                <el-button
                  link
                  type="primary"
                  :disabled="scope.row.status === 'archived'"
                  :loading="archivingRecordUid === scope.row.recordUid"
                  @click="archivePersonalRecord(scope.row)"
                >
                  写入 Obsidian
                </el-button>
                <el-button
                  v-if="scope.row.obsidianUri"
                  tag="a"
                  link
                  type="success"
                  :href="scope.row.obsidianUri"
                >
                  打开
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-row">
          <el-pagination
            v-if="personalRecordTotal > personalRecordPageSize"
            background
            layout="prev, pager, next"
            :current-page="personalRecordPage"
            :page-size="personalRecordPageSize"
            :total="personalRecordTotal"
            @current-change="handlePersonalRecordPageChange"
          />
        </div>
      </el-card>

      <el-card v-if="activePage === 'advanced'" shadow="never">
        <template #header>
          <div class="section-title">
            <div class="card-title">
              <el-icon><Connection /></el-icon>
              MCP Preview
            </div>
            <el-button :loading="mcpToolsLoading || mcpCallsLoading" @click="refreshMcpPreview">
              <el-icon><Refresh /></el-icon>
              刷新 MCP
            </el-button>
          </div>
        </template>

        <div class="mcp-board">
          <div class="mcp-tools-pane">
            <div class="card-title">工具清单</div>
            <el-table
              v-loading="mcpToolsLoading"
              :data="mcpTools"
              border
              empty-text="暂无 MCP 工具"
            >
              <el-table-column prop="name" label="tool" min-width="170" show-overflow-tooltip />
              <el-table-column prop="enabled" label="enabled" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.enabled ? 'success' : 'info'" effect="plain">
                    {{ scope.row.enabled ? 'enabled' : 'disabled' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="description" min-width="260" show-overflow-tooltip />
            </el-table>
          </div>

          <div class="mcp-calls-pane">
            <div class="mcp-filter-row">
              <el-select
                v-model="mcpCallToolFilter"
                clearable
                placeholder="tool"
                @change="handleMcpFilterChange"
              >
                <el-option
                  v-for="tool in mcpTools"
                  :key="tool.name"
                  :label="tool.name"
                  :value="tool.name"
                />
              </el-select>
              <el-select
                v-model="mcpCallStatusFilter"
                clearable
                placeholder="status"
                @change="handleMcpFilterChange"
              >
                <el-option label="completed" value="completed" />
                <el-option label="failed" value="failed" />
              </el-select>
              <el-input
                v-model="mcpCallCallerTypeFilter"
                clearable
                placeholder="callerType"
                @change="handleMcpFilterChange"
                @clear="handleMcpFilterChange"
              />
            </div>

            <el-table
              v-loading="mcpCallsLoading"
              :data="mcpCalls"
              border
              empty-text="暂无 MCP 调用日志"
            >
              <el-table-column prop="createdAt" label="createdAt" min-width="180" show-overflow-tooltip />
              <el-table-column prop="toolName" label="tool" min-width="170" show-overflow-tooltip />
              <el-table-column prop="status" label="status" width="120">
                <template #default="scope">
                  <el-tag :type="mcpStatusTagType(scope.row.status)">
                    {{ scope.row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="caller" min-width="180" show-overflow-tooltip>
                <template #default="scope">
                  {{ scope.row.callerType }} / {{ scope.row.callerId }}
                </template>
              </el-table-column>
              <el-table-column prop="durationMs" label="ms" width="90" align="right" />
              <el-table-column prop="errorCode" label="error" min-width="140" show-overflow-tooltip />
            </el-table>

            <div class="pagination-row">
              <el-pagination
                v-if="mcpCallTotal > mcpCallPageSize"
                background
                layout="prev, pager, next"
                :current-page="mcpCallPage"
                :page-size="mcpCallPageSize"
                :total="mcpCallTotal"
                @current-change="handleMcpCallPageChange"
              />
            </div>
          </div>
        </div>
      </el-card>

      <el-card v-if="activePage === 'advanced'" shadow="never">
        <template #header>
          <div class="section-title">
            <div class="card-title">
              <el-icon><SetUp /></el-icon>
              Vector Export
            </div>
            <el-button :loading="vectorExportsLoading" @click="refreshVectorExports">
              <el-icon><Refresh /></el-icon>
              刷新导出
            </el-button>
          </div>
        </template>

        <el-form class="vector-export-form" label-position="top">
          <el-form-item label="scope">
            <el-select v-model="vectorExportForm.scope">
              <el-option label="sources" value="sources" />
              <el-option label="personal_records" value="personal_records" />
              <el-option label="all" value="all" />
            </el-select>
          </el-form-item>
          <el-form-item label="targetCollection">
            <el-input v-model="vectorExportForm.targetCollection" clearable />
          </el-form-item>
          <el-form-item label="maxChunkChars">
            <el-input-number
              v-model="vectorExportForm.maxChunkChars"
              :min="200"
              :max="8000"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item label="limit">
            <el-input-number
              v-model="vectorExportForm.limit"
              :min="1"
              :max="10000"
              controls-position="right"
            />
          </el-form-item>
          <div class="form-actions">
            <el-tag effect="plain">advanced diagnostic pipeline</el-tag>
            <el-button :loading="vectorExportCreating" type="primary" @click="createVectorExportJob">
              创建导出
            </el-button>
          </div>
        </el-form>

        <el-table
          v-loading="vectorExportsLoading"
          :data="vectorExports"
          border
          empty-text="暂无 Vector Export 记录"
        >
          <el-table-column prop="createdAt" label="createdAt" min-width="170" show-overflow-tooltip />
          <el-table-column prop="exportUid" label="exportUid" min-width="190" show-overflow-tooltip />
          <el-table-column prop="scope" label="scope" width="140" />
          <el-table-column prop="status" label="status" width="120">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'completed' ? 'success' : 'warning'">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="totalCount" label="chunks" width="100" align="right" />
          <el-table-column prop="targetCollection" label="collection" min-width="180" show-overflow-tooltip />
          <el-table-column prop="exportRelativePath" label="path" min-width="220" show-overflow-tooltip />
        </el-table>

        <div class="pagination-row">
          <el-pagination
            v-if="vectorExportTotal > vectorExportPageSize"
            background
            layout="prev, pager, next"
            :current-page="vectorExportPage"
            :page-size="vectorExportPageSize"
            :total="vectorExportTotal"
            @current-change="handleVectorExportPageChange"
          />
        </div>
      </el-card>

      <el-card v-if="activePage === 'inbox'" shadow="never">
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
              <el-tag
                :class="statusBadgeClass(scope.row.status)"
                :type="statusTagType(scope.row.status)"
                effect="plain"
              >
                {{ statusLabels[scope.row.status as ImportJobStatus] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="totalCount" label="total" width="86" align="right" />
          <el-table-column prop="successCount" label="success" width="94" align="right" />
          <el-table-column prop="skippedCount" label="skipped" width="94" align="right" />
          <el-table-column prop="failedCount" label="failed" width="86" align="right" />
          <el-table-column prop="inputPath" label="sourcePath" min-width="220" show-overflow-tooltip />
          <el-table-column prop="rawSourcesRoot" label="归集仓库" min-width="220" show-overflow-tooltip />
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

      <el-card v-if="activePage === 'inbox'" shadow="never">
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

        <div class="ai-config-strip">
          <el-input
            v-model="aiProviderForm.providerName"
            clearable
            placeholder="providerName"
          />
          <el-select v-model="aiProviderForm.providerType" placeholder="providerType">
            <el-option label="OpenAI Compatible" value="openai_compatible" />
            <el-option label="Rule Based" value="rule_based" />
          </el-select>
          <el-input
            v-model="aiProviderForm.modelName"
            clearable
            placeholder="modelName"
          />
          <el-input
            v-model="aiProviderForm.baseUrl"
            clearable
            placeholder="baseUrl 可留空读取环境变量"
          />
          <el-tag effect="plain">{{ aiProviderForm.configSource }}</el-tag>
        </div>

        <div v-if="selectedJobDetail" class="job-detail-strip">
          <span class="detail-item">{{ selectedJobDetail.jobUid }}</span>
          <el-tag
            :class="statusBadgeClass(selectedJobDetail.status)"
            :type="statusTagType(selectedJobDetail.status)"
            effect="plain"
          >
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
          <el-table-column fixed="right" label="操作" width="300">
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
                <el-button
                  link
                  type="warning"
                  @click="selectSourceFileForWikiCompile(scope.row)"
                >
                  <el-icon><EditPen /></el-icon>
                  Wiki 编译
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

      <el-card v-if="activePage === 'wiki-compile'" shadow="never">
        <template #header>
          <div class="section-title">
            <div class="card-title">
              <el-icon><EditPen /></el-icon>
              Topic / Project Wiki 页面
            </div>
            <el-button :loading="wikiPagesLoading || wikiIntegrationsLoading" @click="refreshWikiCompile">
              <el-icon><Refresh /></el-icon>
              刷新 Wiki
            </el-button>
          </div>
        </template>

        <div class="wiki-compile-grid">
          <div class="wiki-panel">
            <div class="card-title">注册用户确认的目标页</div>
            <el-form class="lifeos-form" label-position="top">
              <div class="record-form-row">
                <el-form-item label="pageType">
                  <el-select v-model="wikiPageForm.pageType">
                    <el-option label="topic" value="topic" />
                    <el-option label="project" value="project" />
                  </el-select>
                </el-form-item>
                <el-form-item label="status">
                  <el-select v-model="wikiPageForm.status">
                    <el-option label="active" value="active" />
                    <el-option label="archived" value="archived" />
                  </el-select>
                </el-form-item>
              </div>
              <el-form-item label="标题" required>
                <el-input v-model="wikiPageForm.title" clearable placeholder="WikiForge 产品路线" />
              </el-form-item>
              <el-form-item label="slug">
                <el-input v-model="wikiPageForm.slug" clearable placeholder="wikiforge-roadmap" />
              </el-form-item>
              <el-form-item label="vaultPath" required>
                <el-input v-model="wikiPageForm.vaultPath" clearable placeholder="10_Topics/WikiForge 产品路线.md" />
              </el-form-item>
              <div class="form-actions">
                <el-button :loading="wikiPageCreating" type="primary" @click="createWikiPageFromForm">
                  注册页面
                </el-button>
              </div>
            </el-form>
          </div>

          <div class="wiki-panel">
            <div class="card-title">从 Source File 编译更新</div>
            <el-form class="lifeos-form" label-position="top">
              <el-form-item label="sourceFileUid" required>
                <el-input v-model="wikiCompileForm.fileUid" clearable placeholder="可从待整理资料页选择" />
              </el-form-item>
              <el-form-item label="targetPageUid">
                <el-select v-model="wikiCompileForm.targetPageUid" clearable filterable placeholder="目标页由用户确认">
                  <el-option
                    v-for="page in wikiPages"
                    :key="page.pageUid"
                    :label="`${page.pageType} / ${page.title}`"
                    :value="page.pageUid"
                  />
                </el-select>
              </el-form-item>
              <div class="record-form-row">
                <el-form-item label="riskLevel">
                  <el-select v-model="wikiCompileForm.riskLevel">
                    <el-option label="low" value="low" />
                    <el-option label="medium" value="medium" />
                    <el-option label="high" value="high" />
                  </el-select>
                </el-form-item>
                <el-form-item label="confidenceScore">
                  <el-input-number
                    v-model="wikiCompileForm.confidenceScore"
                    :min="0"
                    :max="1"
                    :step="0.01"
                    controls-position="right"
                  />
                </el-form-item>
              </div>
              <el-form-item label="changeSummary">
                <el-input v-model="wikiCompileForm.changeSummary" clearable placeholder="本次建议追加的内容摘要" />
              </el-form-item>
              <el-form-item label="proposedMarkdown">
                <el-input
                  v-model="wikiCompileForm.proposedMarkdown"
                  type="textarea"
                  :rows="4"
                  resize="vertical"
                  placeholder="留空时由后端生成最小追加块"
                />
              </el-form-item>
              <div class="form-actions">
                <el-tag effect="plain">只追加 WikiForge Updates 托管区块</el-tag>
                <el-button :loading="wikiCompileLoading" type="primary" @click="runWikiCompile">
                  运行 Wiki 编译
                </el-button>
              </div>
            </el-form>
          </div>
        </div>

        <div class="wiki-table-block">
          <div class="section-title compact">
            <div class="card-title">Wiki 页面</div>
            <el-tag effect="plain">{{ wikiPageTotal }} pages</el-tag>
          </div>
          <el-table v-loading="wikiPagesLoading" :data="wikiPages" border empty-text="暂无 Wiki 页面">
            <el-table-column prop="pageType" label="type" width="110" />
            <el-table-column prop="title" label="title" min-width="180" show-overflow-tooltip />
            <el-table-column prop="pageUid" label="pageUid" min-width="190" show-overflow-tooltip />
            <el-table-column prop="vaultPath" label="vaultPath" min-width="260" show-overflow-tooltip />
            <el-table-column prop="status" label="status" width="110">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'active' ? 'success' : 'info'">
                  {{ scope.row.status }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-row">
            <el-pagination
              v-if="wikiPageTotal > wikiPagePageSize"
              background
              layout="prev, pager, next"
              :current-page="wikiPagePage"
              :page-size="wikiPagePageSize"
              :total="wikiPageTotal"
              @current-change="handleWikiPagePageChange"
            />
          </div>
        </div>

        <div class="wiki-table-block">
          <div class="section-title compact">
            <div class="card-title">AI 更新建议与写入记录</div>
            <div class="wiki-filter-row">
              <el-select
                v-model="wikiIntegrationStatusFilter"
                clearable
                placeholder="status"
                @change="handleWikiIntegrationFilterChange"
              >
                <el-option label="pending_review" value="pending_review" />
                <el-option label="auto_applied" value="auto_applied" />
                <el-option label="approved" value="approved" />
                <el-option label="rejected" value="rejected" />
              </el-select>
              <el-input
                v-model="wikiIntegrationPageFilter"
                clearable
                placeholder="pageUid"
                @change="handleWikiIntegrationFilterChange"
                @clear="handleWikiIntegrationFilterChange"
              />
              <el-input
                v-model="wikiIntegrationSourceFilter"
                clearable
                placeholder="sourceUid"
                @change="handleWikiIntegrationFilterChange"
                @clear="handleWikiIntegrationFilterChange"
              />
            </div>
          </div>

          <el-table
            v-loading="wikiIntegrationsLoading"
            :data="wikiIntegrations"
            border
            empty-text="暂无 Wiki 更新记录"
          >
            <el-table-column prop="createdAt" label="createdAt" min-width="170" show-overflow-tooltip />
            <el-table-column prop="status" label="status" width="130">
              <template #default="scope">
                <el-tag :type="wikiIntegrationStatusTagType(scope.row.status)">
                  {{ scope.row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="pageTitle" label="page" min-width="180" show-overflow-tooltip />
            <el-table-column prop="sourceFileUid" label="sourceFile" min-width="160" show-overflow-tooltip />
            <el-table-column prop="riskLevel" label="risk" width="100" />
            <el-table-column prop="confidenceScore" label="confidence" width="120" align="right" />
            <el-table-column prop="changeSummary" label="summary" min-width="240" show-overflow-tooltip />
            <el-table-column label="actions" width="180" fixed="right">
              <template #default="scope">
                <template v-if="scope.row.status === 'pending_review'">
                  <el-button
                    link
                    type="success"
                    :loading="wikiDecisionLoadingUid === scope.row.integrationUid"
                    @click="approveWikiUpdate(scope.row)"
                  >
                    通过
                  </el-button>
                  <el-button
                    link
                    type="info"
                    :loading="wikiDecisionLoadingUid === scope.row.integrationUid"
                    @click="rejectWikiUpdate(scope.row)"
                  >
                    拒绝
                  </el-button>
                </template>
                <el-tag v-else effect="plain">{{ scope.row.appliedAt || 'closed' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-row">
            <el-pagination
              v-if="wikiIntegrationTotal > wikiIntegrationPageSize"
              background
              layout="prev, pager, next"
              :current-page="wikiIntegrationPage"
              :page-size="wikiIntegrationPageSize"
              :total="wikiIntegrationTotal"
              @current-change="handleWikiIntegrationPageChange"
            />
          </div>
        </div>
      </el-card>

      <el-card v-if="activePage === 'obsidian'" shadow="never">
        <template #header>
          <div class="section-title">
            <div class="card-title">
              <el-icon><FolderAdd /></el-icon>
              Obsidian / Wiki 页面
            </div>
            <el-button :loading="vaultStatusLoading || wikiPagesLoading" @click="refreshWikiCompile">
              <el-icon><Refresh /></el-icon>
              刷新页面
            </el-button>
          </div>
        </template>

        <div class="obsidian-page-grid">
          <div class="wiki-panel">
            <div class="card-title">Vault 状态</div>
            <p class="metric" :class="{ ok: vaultStatus?.exists && vaultStatus?.writable }">
              {{ vaultStatusLabel }}
            </p>
            <p class="muted truncate" :title="vaultStatus?.vaultPath || ''">
              {{ vaultStatus?.vaultPath || '等待检测' }}
            </p>
          </div>
          <div class="wiki-panel">
            <div class="card-title">写入边界</div>
            <p class="muted">
              自动写入只追加 WikiForge Updates 托管区块；Topic / Project 页面仍由用户创建、确认和长期维护。
            </p>
          </div>
        </div>

        <el-table v-loading="wikiPagesLoading" :data="wikiPages" border empty-text="暂无 Wiki 页面">
          <el-table-column prop="pageType" label="type" width="110" />
          <el-table-column prop="title" label="title" min-width="180" show-overflow-tooltip />
          <el-table-column prop="vaultPath" label="vaultPath" min-width="280" show-overflow-tooltip />
          <el-table-column prop="status" label="status" width="120">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'active' ? 'success' : 'info'">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="createdAt" min-width="170" show-overflow-tooltip />
        </el-table>
      </el-card>

      <el-card v-if="activePage === 'review'" shadow="never">
        <template #header>
          <div class="section-title">
            <div class="card-title">
              <el-icon><EditPen /></el-icon>
              Wiki 更新审核
            </div>
            <el-button :loading="wikiIntegrationsLoading" @click="refreshWikiReviewQueue">
              <el-icon><Refresh /></el-icon>
              刷新 Wiki 审核
            </el-button>
          </div>
        </template>

        <el-table
          v-loading="wikiIntegrationsLoading"
          :data="wikiIntegrations.filter((item) => item.status === 'pending_review')"
          border
          empty-text="暂无待审核 Wiki 更新"
        >
          <el-table-column prop="createdAt" label="createdAt" min-width="170" show-overflow-tooltip />
          <el-table-column prop="pageTitle" label="page" min-width="180" show-overflow-tooltip />
          <el-table-column prop="sourceFileUid" label="sourceFile" min-width="160" show-overflow-tooltip />
          <el-table-column prop="riskLevel" label="risk" width="100" />
          <el-table-column prop="confidenceScore" label="confidence" width="120" align="right" />
          <el-table-column prop="changeSummary" label="summary" min-width="240" show-overflow-tooltip />
          <el-table-column fixed="right" label="操作" width="160">
            <template #default="scope">
              <el-button
                link
                type="success"
                :loading="wikiDecisionLoadingUid === scope.row.integrationUid"
                @click="approveWikiUpdate(scope.row)"
              >
                通过
              </el-button>
              <el-button
                link
                type="info"
                :loading="wikiDecisionLoadingUid === scope.row.integrationUid"
                @click="rejectWikiUpdate(scope.row)"
              >
                拒绝
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card v-if="activePage === 'review'" shadow="never">
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

        <div v-if="selectedReviewItem.status === 'pending'" class="review-actions">
          <el-button
            type="success"
            :loading="approvingReviewUid === selectedReviewItem.reviewUid"
            @click="approveSelectedReviewItem"
          >
            <el-icon><EditPen /></el-icon>
            通过并写入 Obsidian
          </el-button>
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
    </section>
  </main>
</template>
