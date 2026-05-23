import { http, type ApiResponse } from '../../services/http';
import type {
  AiReviewRun,
  CreateAiReviewRunRequest,
  ReviewItemListParams,
  ReviewItemPage
} from '../../types/review';

function unwrapResponse<T>(response: ApiResponse<T | null>): T {
  if (!response.success || response.data === null) {
    throw new Error(response.message || 'Request failed');
  }
  return response.data;
}

export async function createAiReviewRun(
  fileUid: string,
  payload: CreateAiReviewRunRequest
): Promise<AiReviewRun> {
  const response = await http.post<ApiResponse<AiReviewRun | null>>(
    `/v1/source-files/${encodeURIComponent(fileUid)}/ai-review-runs`,
    payload
  );
  return unwrapResponse(response.data);
}

export async function getAiReviewRun(runUid: string): Promise<AiReviewRun> {
  const response = await http.get<ApiResponse<AiReviewRun | null>>(
    `/v1/ai-review-runs/${encodeURIComponent(runUid)}`
  );
  return unwrapResponse(response.data);
}

export async function listReviewItems(
  params: ReviewItemListParams = {}
): Promise<ReviewItemPage> {
  const response = await http.get<ApiResponse<ReviewItemPage | null>>('/v1/review-items', {
    params
  });
  return unwrapResponse(response.data);
}
