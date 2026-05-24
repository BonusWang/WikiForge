import { http, type ApiResponse } from '../../services/http';
import type {
  CreateLocalImportJobRequest,
  ImportJob,
  ImportJobDetail,
  ImportJobListParams,
  PageResult
} from '../../types/importJobs';
export { getSourceFile, listSourceFiles } from '../source-files';

function unwrapResponse<T>(response: ApiResponse<T | null>): T {
  if (!response.success || response.data === null) {
    throw new Error(response.message || 'Request failed');
  }
  return response.data;
}

export async function createLocalImportJob(
  payload: CreateLocalImportJobRequest
): Promise<ImportJob> {
  const response = await http.post<ApiResponse<ImportJob | null>>('/v1/import-jobs/local', payload);
  return unwrapResponse(response.data);
}

export async function listImportJobs(
  params: ImportJobListParams = {}
): Promise<PageResult<ImportJob>> {
  const response = await http.get<ApiResponse<PageResult<ImportJob> | null>>('/v1/import-jobs', {
    params
  });
  return unwrapResponse(response.data);
}

export async function getImportJob(jobUid: string): Promise<ImportJobDetail> {
  const response = await http.get<ApiResponse<ImportJobDetail | null>>(
    `/v1/import-jobs/${encodeURIComponent(jobUid)}`
  );
  return unwrapResponse(response.data);
}
