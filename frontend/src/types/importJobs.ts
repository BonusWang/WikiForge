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
  inputPathMasked?: string;
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
  duplicateCount?: number;
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

export interface SourceFile {
  fileUid: string;
  sourceUid: string;
  jobUid: string;
  fileName: string;
  originalName?: string | null;
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
  obsidianNoteUid?: string | null;
  obsidianNoteStatus?: string | null;
  obsidianNoteTitle?: string | null;
  obsidianVaultPath?: string | null;
  obsidianUri?: string | null;
  obsidianNoteCreatedAt?: string | null;
  createdAt: string;
}
