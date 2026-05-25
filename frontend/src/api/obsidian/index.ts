import { http, type ApiResponse } from '../../services/http';
import type {
  ObsidianInitResult,
  ObsidianVaultStatus
} from '../../types/obsidianVault';

function unwrapResponse<T>(response: ApiResponse<T | null>): T {
  if (!response.success || response.data === null) {
    throw new Error(response.message || 'Request failed');
  }
  return response.data;
}

export async function initializeObsidianVault(): Promise<ObsidianInitResult> {
  const response = await http.post<ApiResponse<ObsidianInitResult | null>>('/v1/obsidian/init');
  return unwrapResponse(response.data);
}

export async function getObsidianStatus(): Promise<ObsidianVaultStatus> {
  const response = await http.get<ApiResponse<ObsidianVaultStatus | null>>('/v1/obsidian/status');
  return unwrapResponse(response.data);
}
