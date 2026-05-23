export interface McpToolDefinition {
  name: string;
  description: string;
  enabled: boolean;
  inputSchema: Record<string, unknown>;
  outputSchema: Record<string, unknown>;
}

export interface McpToolListResponse {
  tools: McpToolDefinition[];
}

export type McpCallStatus = 'completed' | 'failed' | string;

export interface McpToolCallLog {
  callUid: string;
  toolName: string;
  callerType: string;
  callerId: string;
  status: McpCallStatus;
  errorCode?: string | null;
  errorMessage?: string | null;
  durationMs: number;
  createdAt: string;
}

export interface McpToolCallPageResponse {
  items: McpToolCallLog[];
  page: number;
  pageSize: number;
  total: number;
}

export interface McpToolCallListParams {
  toolName?: string;
  status?: McpCallStatus;
  callerType?: string;
  page?: number;
  pageSize?: number;
}
