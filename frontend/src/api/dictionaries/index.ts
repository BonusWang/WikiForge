import { http, type ApiResponse } from '../../services/http';
import type {
  DictionaryItem,
  DictionaryListParams,
  DictionaryListResponse
} from '../../types/dictionaries';

function unwrapResponse<T>(response: ApiResponse<T | null>): T {
  if (!response.success || response.data === null) {
    throw new Error(response.message || 'Request failed');
  }
  return response.data;
}

export async function listDictionaries(
  params: DictionaryListParams = {}
): Promise<DictionaryItem[]> {
  const response = await http.get<ApiResponse<DictionaryListResponse | null>>('/v1/dictionaries', {
    params
  });
  return unwrapResponse(response.data).items;
}
