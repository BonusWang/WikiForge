import { http, type ApiResponse } from '../../services/http';
import type { UploadSourcesRequest, UploadSourcesResult } from '../../types/uploadSources';

function unwrapResponse<T>(response: ApiResponse<T | null>): T {
  if (!response.success || response.data === null) {
    throw new Error(response.message || 'Request failed');
  }
  return response.data;
}

export async function uploadSources(payload: UploadSourcesRequest): Promise<UploadSourcesResult> {
  const formData = new FormData();

  payload.files.forEach((file) => {
    formData.append('files', file);
  });

  if (payload.wikiWritebackMode) {
    formData.append('wikiWritebackMode', payload.wikiWritebackMode);
  }

  const response = await http.post<ApiResponse<UploadSourcesResult | null>>(
    '/v1/upload-sources',
    formData
  );
  return unwrapResponse(response.data);
}
