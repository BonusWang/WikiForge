export type VectorExportScope = 'all' | 'sources' | 'personal_records';

export interface CreateVectorExportRequest {
  scope?: VectorExportScope;
  targetCollection?: string;
  maxChunkChars?: number;
  limit?: number;
}

export interface VectorExportJob {
  exportUid: string;
  scope: VectorExportScope;
  targetCollection: string;
  exportFormat: string;
  status: string;
  totalCount: number;
  exportFileName?: string | null;
  exportRelativePath?: string | null;
  createdAt: string;
  finishedAt?: string | null;
  errorMessage?: string | null;
}

export interface VectorExportPageResponse {
  items: VectorExportJob[];
  page: number;
  pageSize: number;
  total: number;
}

export interface VectorExportListParams {
  status?: string;
  page?: number;
  pageSize?: number;
}
