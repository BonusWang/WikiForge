import { http, type ApiResponse } from '../../services/http';
import type {
  CreateWikiCompileRunRequest,
  CreateWikiPageRequest,
  WikiCompileRunResponse,
  WikiIntegration,
  WikiIntegrationDecisionRequest,
  WikiIntegrationListParams,
  WikiIntegrationPageResponse,
  WikiPage,
  WikiPageListParams,
  WikiPagePageResponse
} from '../../types/wiki';

function unwrapResponse<T>(response: ApiResponse<T | null>): T {
  if (!response.success || response.data === null) {
    throw new Error(response.message || 'Request failed');
  }
  return response.data;
}

export async function createWikiPage(payload: CreateWikiPageRequest): Promise<WikiPage> {
  const response = await http.post<ApiResponse<WikiPage | null>>('/v1/wiki-pages', payload);
  return unwrapResponse(response.data);
}

export async function listWikiPages(
  params: WikiPageListParams = {}
): Promise<WikiPagePageResponse> {
  const response = await http.get<ApiResponse<WikiPagePageResponse | null>>('/v1/wiki-pages', {
    params
  });
  return unwrapResponse(response.data);
}

export async function createWikiCompileRun(
  fileUid: string,
  payload: CreateWikiCompileRunRequest = {}
): Promise<WikiCompileRunResponse> {
  const response = await http.post<ApiResponse<WikiCompileRunResponse | null>>(
    `/v1/source-files/${encodeURIComponent(fileUid)}/wiki-compile-runs`,
    payload
  );
  return unwrapResponse(response.data);
}

export async function listWikiIntegrations(
  params: WikiIntegrationListParams = {}
): Promise<WikiIntegrationPageResponse> {
  const response = await http.get<ApiResponse<WikiIntegrationPageResponse | null>>('/v1/wiki-integrations', {
    params
  });
  return unwrapResponse(response.data);
}

export async function approveWikiIntegration(
  integrationUid: string,
  payload: WikiIntegrationDecisionRequest = {}
): Promise<WikiIntegration> {
  const response = await http.post<ApiResponse<WikiIntegration | null>>(
    `/v1/wiki-integrations/${encodeURIComponent(integrationUid)}/approve`,
    payload
  );
  return unwrapResponse(response.data);
}

export async function rejectWikiIntegration(
  integrationUid: string,
  payload: WikiIntegrationDecisionRequest = {}
): Promise<WikiIntegration> {
  const response = await http.post<ApiResponse<WikiIntegration | null>>(
    `/v1/wiki-integrations/${encodeURIComponent(integrationUid)}/reject`,
    payload
  );
  return unwrapResponse(response.data);
}
