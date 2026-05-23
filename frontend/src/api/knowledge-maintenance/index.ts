import { http, type ApiResponse } from '../../services/http';
import type {
  CreateKnowledgeMaintenanceRunRequest,
  KnowledgeMaintenanceItemListParams,
  KnowledgeMaintenanceItemPageResponse,
  KnowledgeMaintenanceRun,
  KnowledgeMaintenanceRunListParams,
  KnowledgeMaintenanceRunPageResponse
} from '../../types/knowledgeMaintenance';

function unwrapResponse<T>(response: ApiResponse<T | null>): T {
  if (!response.success || response.data === null) {
    throw new Error(response.message || 'Request failed');
  }
  return response.data;
}

export async function createKnowledgeMaintenanceRun(
  payload: CreateKnowledgeMaintenanceRunRequest
): Promise<KnowledgeMaintenanceRun> {
  const response = await http.post<ApiResponse<KnowledgeMaintenanceRun | null>>('/v1/maintenance-runs', payload);
  return unwrapResponse(response.data);
}

export async function listKnowledgeMaintenanceRuns(
  params: KnowledgeMaintenanceRunListParams = {}
): Promise<KnowledgeMaintenanceRunPageResponse> {
  const response = await http.get<ApiResponse<KnowledgeMaintenanceRunPageResponse | null>>('/v1/maintenance-runs', {
    params
  });
  return unwrapResponse(response.data);
}

export async function listKnowledgeMaintenanceItems(
  params: KnowledgeMaintenanceItemListParams = {}
): Promise<KnowledgeMaintenanceItemPageResponse> {
  const response = await http.get<ApiResponse<KnowledgeMaintenanceItemPageResponse | null>>('/v1/maintenance-items', {
    params
  });
  return unwrapResponse(response.data);
}
