import axios from 'axios';

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000
});

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
  code?: string | null;
}

export interface BackendHealth {
  service: string;
  status: string;
  timestamp: string;
}

export async function fetchBackendHealth(): Promise<BackendHealth> {
  const response = await http.get<ApiResponse<BackendHealth>>('/health');
  return response.data.data;
}
