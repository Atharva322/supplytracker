import { AxiosError } from 'axios';

export interface ApiError {
  status?: number;
  message: string;
  code: 'UNAUTHORIZED' | 'FORBIDDEN' | 'NOT_FOUND' | 'SERVER_ERROR' | 'NETWORK_ERROR' | 'UNKNOWN';
}

export function normalizeApiError(error: unknown): ApiError {
  if (error instanceof AxiosError) {
    const status = error.response?.status;
    if (status === 401) return { status, code: 'UNAUTHORIZED', message: 'Authentication required' };
    if (status === 403) return { status, code: 'FORBIDDEN', message: 'Access forbidden' };
    if (status === 404) return { status, code: 'NOT_FOUND', message: 'Resource not found' };
    if (status && status >= 500) return { status, code: 'SERVER_ERROR', message: 'Server error' };
    if (error.request && !error.response) return { code: 'NETWORK_ERROR', message: 'Network error' };
    return { status, code: 'UNKNOWN', message: error.message || 'Request failed' };
  }
  return { code: 'UNKNOWN', message: error instanceof Error ? error.message : 'Unknown error' };
}
