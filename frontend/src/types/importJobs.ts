export type ImportJobStatus = 'pending' | 'running' | 'completed' | 'failed' | 'cancelled';

export interface CreateLocalImportJobRequest {
  inputPath: string;
  rawSourcesRoot: string;
  recursive: boolean;
  organizeMode: 'copy';
  maxCopyFileSizeMb: number;
}

export interface ImportJob {
  jobUid: string;
  importType: string;
  inputPath: string;
  rawSourcesRoot: string;
  recursive: boolean;
  organizeMode: 'copy';
  status: ImportJobStatus;
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
  page?: number;
  pageSize?: number;
}

export interface SourceFileListParams {
  jobUid: string;
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
  fileExt: string;
  originalPath: string;
  managedPath: string;
  fileSize: number;
  mimeType?: string | null;
  contentHash: string;
  parseStatus?: string | null;
  organizeStatus: string;
  duplicateOfFileUid?: string | null;
  obsidianNoteUid?: string | null;
  obsidianNoteStatus?: string | null;
  obsidianNoteTitle?: string | null;
  obsidianVaultPath?: string | null;
  obsidianUri?: string | null;
  obsidianNoteCreatedAt?: string | null;
  createdAt: string;
}
