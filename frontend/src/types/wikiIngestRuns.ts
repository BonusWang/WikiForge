import type { PageResult } from './importJobs';

export interface WikiIngestRun {
  runUid: string;
  fileUid: string;
  fileName?: string | null;
  statusCode: string;
  statusLabel: string;
  sourcePagePath?: string | null;
  indexUpdated: boolean;
  logEntryAppended: boolean;
  writeStatusCode?: string | null;
  writeStatusLabel?: string | null;
  failureReason?: string | null;
  createdAt: string;
  completedAt?: string | null;
}

export interface WikiIngestRunDetail extends WikiIngestRun {
  managedBlockPreview?: string | null;
  logEntryPreview?: string | null;
  obsidianUri?: string | null;
  retryable: boolean;
}

export interface WikiIngestRunListParams {
  statusCode?: string;
  fileUid?: string;
  page?: number;
  pageSize?: number;
}

export type WikiIngestRunPage = PageResult<WikiIngestRun>;
