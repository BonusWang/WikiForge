export interface UploadSourcesRequest {
  files: File[];
}

export interface UploadSourcesResult {
  jobUid: string;
  importType: '浏览器上传' | string;
  statusCode: string;
  statusLabel: string;
  statusDescription?: string;
  uploadedCount: number;
  createdAt: string;
}
