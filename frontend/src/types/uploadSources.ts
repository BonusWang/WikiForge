export interface UploadSourcesRequest {
  files: File[];
  wikiWritebackMode?: '自动' | '关闭';
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
