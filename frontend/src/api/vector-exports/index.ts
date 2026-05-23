import { http, type ApiResponse } from '../../services/http';
import type {
  CreateVectorExportRequest,
  VectorExportJob,
  VectorExportListParams,
  VectorExportPageResponse
} from '../../types/vectorExports';

function unwrapResponse<T>(response: ApiResponse<T | null>): T {
  if (!response.success || response.data === null) {
    throw new Error(response.message || 'Request failed');
  }
  return response.data;
}

export async function createVectorExport(
  payload: CreateVectorExportRequest
): Promise<VectorExportJob> {
  const response = await http.post<ApiResponse<VectorExportJob | null>>('/v1/vector-exports', payload);
  return unwrapResponse(response.data);
}

export async function listVectorExports(
  params: VectorExportListParams = {}
): Promise<VectorExportPageResponse> {
  const response = await http.get<ApiResponse<VectorExportPageResponse | null>>('/v1/vector-exports', {
    params
  });
  return unwrapResponse(response.data);
}
