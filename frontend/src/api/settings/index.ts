import { http, type ApiResponse } from '../../services/http';
import type { AppSettings, UpdateAppSettingsRequest } from '../../types/settings';

function unwrapResponse<T>(response: ApiResponse<T | null>): T {
  if (!response.success || response.data === null) {
    throw new Error(response.message || 'Request failed');
  }
  return response.data;
}

export async function getSettings(): Promise<AppSettings> {
  const response = await http.get<ApiResponse<AppSettings | null>>('/v1/settings');
  return unwrapResponse(response.data);
}

export async function updateSettings(payload: UpdateAppSettingsRequest): Promise<AppSettings> {
  const response = await http.put<ApiResponse<AppSettings | null>>('/v1/settings', payload);
  return unwrapResponse(response.data);
}
