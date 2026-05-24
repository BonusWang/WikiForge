export interface CreateKnowledgeMaintenanceRunRequest {
  staleDays?: number;
  limit?: number;
}

export interface KnowledgeMaintenanceRun {
  runUid: string;
  runType: string;
  status: string;
  staleDays: number;
  totalCount: number;
  issueCount: number;
  startedAt: string;
  finishedAt?: string | null;
  createdAt: string;
  errorMessage?: string | null;
}

export interface KnowledgeMaintenanceRunPageResponse {
  items: KnowledgeMaintenanceRun[];
  page: number;
  pageSize: number;
  total: number;
}

export interface KnowledgeMaintenanceItem {
  itemUid: string;
  runUid: string;
  issueType: string;
  severity: string;
  contentType: string;
  sourceUid?: string | null;
  fileUid?: string | null;
  recordUid?: string | null;
  chunkUid?: string | null;
  exportUid?: string | null;
  title?: string | null;
  summary: string;
  evidenceJson?: string | null;
  status: string;
  resolutionNote?: string | null;
  resolvedBy?: string | null;
  resolvedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeMaintenanceItemPageResponse {
  items: KnowledgeMaintenanceItem[];
  page: number;
  pageSize: number;
  total: number;
}

export interface KnowledgeMaintenanceRunListParams {
  status?: string;
  page?: number;
  pageSize?: number;
}

export interface KnowledgeMaintenanceItemListParams {
  runUid?: string;
  issueType?: string;
  severity?: string;
  status?: string;
  page?: number;
  pageSize?: number;
}

export interface UpdateKnowledgeMaintenanceItemStatusRequest {
  status: 'open' | 'resolved' | 'ignored';
  resolutionNote?: string;
  resolvedBy?: string;
}
