import { http, type ApiResponse } from '../../services/http';
import type { PageResult, SourceFile, SourceFileListParams } from '../../types/importJobs';

function unwrapResponse<T>(response: ApiResponse<T | null>): T {
  if (!response.success || response.data === null) {
    throw new Error(response.message || 'Request failed');
  }
  return response.data;
}

export async function listSourceFiles(
  params: SourceFileListParams = {}
): Promise<PageResult<SourceFile>> {
  const response = await http.get<ApiResponse<PageResult<SourceFile> | null>>('/v1/source-files', {
    params
  });
  return unwrapResponse(response.data);
}

export async function getSourceFile(fileUid: string): Promise<SourceFile> {
  const response = await http.get<ApiResponse<SourceFile | null>>(
    `/v1/source-files/${encodeURIComponent(fileUid)}`
  );
  return unwrapResponse(response.data);
}
