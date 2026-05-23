import { http, type ApiResponse } from '../../services/http';
import type {
  McpToolCallListParams,
  McpToolCallPageResponse,
  McpToolListResponse
} from '../../types/mcp';

function unwrapResponse<T>(response: ApiResponse<T | null>): T {
  if (!response.success || response.data === null) {
    throw new Error(response.message || 'Request failed');
  }
  return response.data;
}

export async function listMcpTools(): Promise<McpToolListResponse> {
  const response = await http.get<ApiResponse<McpToolListResponse | null>>('/v1/mcp/tools');
  return unwrapResponse(response.data);
}

export async function listMcpCalls(
  params: McpToolCallListParams = {}
): Promise<McpToolCallPageResponse> {
  const response = await http.get<ApiResponse<McpToolCallPageResponse | null>>('/v1/mcp/calls', {
    params
  });
  return unwrapResponse(response.data);
}
