export type ImportJobStatus = 'pending' | 'running' | 'completed' | 'failed' | 'cancelled';

export interface CreateLocalImportJobRequest {
  inputPath: string;
  rawSourcesRoot?: string;
  recursive: boolean;
  organizeMode: 'copy';
  maxCopyFileSizeMb: number;
}

export interface ImportJob {
  jobUid: string;
  importType: string;
  inputPath?: string;
  rawSourcesRoot?: string;
  recursive: boolean;
  organizeMode: 'copy';
  status: ImportJobStatus;
  statusCode?: string;
  statusLabel?: string;
  statusDescription?: string;
  statusColor?: string;
  isTerminal?: boolean;
  totalCount: number;
  successCount: number;
  skippedCount: number;
  failedCount: number;
  createdAt: string;
}

export interface ImportJobDetail extends ImportJob {
  startedAt?: string | null;
  finishedAt?: string | null;
  errorMessage?: string | null;
}

export interface ImportJobListParams {
  status?: ImportJobStatus;
  statusCode?: string;
  page?: number;
  pageSize?: number;
}

export interface SourceFileListParams {
  jobUid?: string;
  collectStatusCode?: string;
  extractStatusCode?: string;
  wikiStatusCode?: string;
  page?: number;
  pageSize?: number;
}

export interface PageResult<T> {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
}

export interface SourceFileLatestWikiIngestRun {
  runUid: string;
  fileUid: string;
  fileName?: string | null;
  statusCode: string;
  statusLabel: string;
  sourcePagePath?: string | null;
  wikiPagePaths: string[];
  indexUpdated: boolean;
  logEntryAppended: boolean;
  writeStatusCode?: string | null;
  writeStatusLabel?: string | null;
  fallbackReason?: string | null;
  failureReason?: string | null;
  obsidianUri?: string | null;
  retryable?: boolean;
  createdAt: string;
  completedAt?: string | null;
}

export interface SourceFile {
  fileUid: string;
  jobUid: string;
  fileName: string;
  fileExt: string;
  originalPath?: string;
  originalPathMasked?: string | null;
  managedPath?: string;
  rawSourceRelativePath?: string;
  fileSize: number;
  fileSizeBytes?: number;
  fileType?: string | null;
  mimeType?: string | null;
  contentHash: string;
  collectStatusCode?: string | null;
  collectStatusLabel?: string | null;
  extractStatusCode?: string | null;
  extractStatusLabel?: string | null;
  wikiStatusCode?: string | null;
  wikiStatusLabel?: string | null;
  parseStatus?: string | null;
  organizeStatus: string;
  duplicateOfFileUid?: string | null;
  extractFailureReason?: string | null;
  wikiFailureReason?: string | null;
  latestWikiIngestRun?: SourceFileLatestWikiIngestRun | null;
  createdAt: string;
}
