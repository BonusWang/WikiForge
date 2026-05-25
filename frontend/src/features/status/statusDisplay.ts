import type { ImportJobStatus } from '../../types/importJobs';

export interface StatusDisplay {
  code: string;
  label: string;
  description: string;
  tagType: 'success' | 'warning' | 'info' | 'primary' | 'danger';
  className: string;
}

const importJobStatusDisplay: Record<ImportJobStatus, StatusDisplay> = {
  pending: {
    code: '已创建',
    label: '已创建',
    description: '任务已创建，等待执行',
    tagType: 'info',
    className: 'status-badge status-badge--pending'
  },
  running: {
    code: '执行中',
    label: '执行中',
    description: '正在收纳和整理文件',
    tagType: 'primary',
    className: 'status-badge status-badge--running'
  },
  completed: {
    code: '已完成',
    label: '已完成',
    description: '任务已完成',
    tagType: 'success',
    className: 'status-badge status-badge--completed'
  },
  failed: {
    code: '失败',
    label: '失败',
    description: '任务执行失败',
    tagType: 'danger',
    className: 'status-badge status-badge--failed'
  },
  cancelled: {
    code: '已取消',
    label: '已取消',
    description: '任务已取消',
    tagType: 'warning',
    className: 'status-badge status-badge--cancelled'
  }
};

export const importJobStatusOptions = Object.entries(importJobStatusDisplay).map(([value, item]) => ({
  value: item.code,
  legacyValue: value as ImportJobStatus,
  label: item.label
}));

const wikiWriteStatusDisplay: Record<string, StatusDisplay> = {
  已创建: {
    code: '已创建',
    label: '已创建',
    description: 'Wiki 写入任务已创建',
    tagType: 'info',
    className: 'status-badge status-badge--pending'
  },
  写入中: {
    code: '写入中',
    label: '写入中',
    description: '正在写入 Obsidian Wiki',
    tagType: 'primary',
    className: 'status-badge status-badge--running'
  },
  已写入: {
    code: '已写入',
    label: '已写入',
    description: '已写入 Obsidian Wiki',
    tagType: 'success',
    className: 'status-badge status-badge--completed'
  },
  失败: {
    code: '失败',
    label: '失败',
    description: 'Wiki 写入失败',
    tagType: 'danger',
    className: 'status-badge status-badge--failed'
  }
};

export function getImportJobStatusDisplay(status: ImportJobStatus | string): StatusDisplay {
  const mappedByInternalValue = importJobStatusDisplay[status as ImportJobStatus];
  if (mappedByInternalValue) {
    return mappedByInternalValue;
  }

  const mappedByChineseCode = Object.values(importJobStatusDisplay).find((item) => {
    return item.code === status || item.label === status;
  });
  if (mappedByChineseCode) {
    return mappedByChineseCode;
  }

  return {
    code: String(status || '未知'),
    label: String(status || '未知'),
    description: '状态字典尚未登记',
    tagType: 'info',
    className: 'status-badge'
  };
}

export function getWikiWriteStatusDisplay(status: string | null | undefined): StatusDisplay {
  const mapped = wikiWriteStatusDisplay[String(status || '')];
  if (mapped) {
    return mapped;
  }

  return {
    code: String(status || '未知'),
    label: String(status || '未知'),
    description: '状态字典尚未登记',
    tagType: 'info',
    className: 'status-badge'
  };
}

export function formatBytes(value: number): string {
  if (!Number.isFinite(value)) {
    return '-';
  }

  const units = ['B', 'KB', 'MB', 'GB'];
  let size = value;
  let unitIndex = 0;

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex += 1;
  }

  return `${size.toFixed(unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
}
