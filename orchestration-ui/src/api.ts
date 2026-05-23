import type { ApiResponse, OrchestrationOverview, OrchestrationTask } from './types';

async function request<T>(path: string): Promise<T> {
  const response = await fetch(path);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  const body = (await response.json()) as ApiResponse<T>;
  if (!body.success) {
    throw new Error(body.message || body.code || 'Request failed');
  }
  return body.data;
}

export function fetchOverview() {
  return request<OrchestrationOverview>('/api/v1/orchestration/overview');
}

export function fetchTasks() {
  return request<OrchestrationTask[]>('/api/v1/orchestration/tasks');
}

export function fetchTask(taskId: string) {
  return request<OrchestrationTask>(`/api/v1/orchestration/tasks/${encodeURIComponent(taskId)}`);
}
