export interface DictionaryItem {
  dictType: string;
  dictCode: string;
  labelZh: string;
  descriptionZh: string;
  sortOrder: number;
  colorToken: 'success' | 'warning' | 'info' | 'primary' | 'danger' | string;
  isTerminal: boolean;
  isSuccess: boolean;
}

export interface DictionaryListResponse {
  items: DictionaryItem[];
}

export interface DictionaryListParams {
  dictType?: string;
}
