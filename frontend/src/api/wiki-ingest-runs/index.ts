import { http, type ApiResponse } from '../../services/http';
import type {
  WikiIngestRun,
  WikiIngestRunDetail,
  WikiIngestRunListParams,
  WikiIngestRunPage
} from '../../types/wikiIngestRuns';

function unwrapResponse<T>(response: ApiResponse<T | null>): T {
  if (!response.success || response.data === null) {
    throw new Error(response.message || 'Request failed');
  }
  return response.data;
}

export async function createWikiIngestRun(
  fileUid: string
): Promise<WikiIngestRun> {
  const response = await http.post<ApiResponse<WikiIngestRun | null>>(
    `/v1/source-files/${encodeURIComponent(fileUid)}/wiki-ingest-runs`
  );
  return unwrapResponse(response.data);
}

export async function listWikiIngestRuns(
  params: WikiIngestRunListParams = {}
): Promise<WikiIngestRunPage> {
  const response = await http.get<ApiResponse<WikiIngestRunPage | null>>(
    '/v1/wiki-ingest-runs',
    { params }
  );
  return unwrapResponse(response.data);
}

export async function getWikiIngestRun(runUid: string): Promise<WikiIngestRunDetail> {
  const response = await http.get<ApiResponse<WikiIngestRunDetail | null>>(
    `/v1/wiki-ingest-runs/${encodeURIComponent(runUid)}`
  );
  return unwrapResponse(response.data);
}
