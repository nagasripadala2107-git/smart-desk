import { API_BASE_URL } from './constants';
import { getStoredToken, clearStoredAuth } from './auth';
import { ApiResponse, ApiErrorResponse } from '@/types';

export class ApiError extends Error {
  status: number;
  code: string;
  validationErrors?: Record<string, string>;

  constructor(status: number, message: string, code = 'API_ERROR', validationErrors?: Record<string, string>) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.validationErrors = validationErrors;
  }
}

interface RequestOptions extends RequestInit {
  params?: Record<string, string | number | boolean | undefined>;
}

async function request<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
  const { params, headers = {}, ...rest } = options;

  let url = `${API_BASE_URL}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;

  if (params) {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        searchParams.append(key, String(value));
      }
    });
    const queryString = searchParams.toString();
    if (queryString) {
      url += (url.includes('?') ? '&' : '?') + queryString;
    }
  }

  const token = getStoredToken();
  const requestHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
    ...(headers as Record<string, string>),
  };

  if (token) {
    requestHeaders['Authorization'] = `Bearer ${token}`;
  }

  let response: Response;
  try {
    response = await fetch(url, {
      ...rest,
      headers: requestHeaders,
    });
  } catch {
    throw new ApiError(0, 'Unable to connect to the SmartDesk server. Please check your network or ensure the backend is running.', 'NETWORK_ERROR');
  }

  if (response.status === 401) {
    clearStoredAuth();
    if (endpoint.includes('/auth/login')) {
      throw new ApiError(401, 'Invalid email or password. Note that passwords are case-sensitive.', 'BAD_CREDENTIALS');
    }
    if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
      window.location.href = `/login?expired=true`;
    }
    throw new ApiError(401, 'Your session has expired. Please sign in again.', 'UNAUTHORIZED');
  }

  let data: unknown;
  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    try {
      data = await response.json();
    } catch {
      data = null;
    }
  }

  if (!response.ok) {
    const errData = data as ApiErrorResponse | null;
    const message = errData?.message || `Request failed with status ${response.status}`;
    const code = errData?.error || 'HTTP_ERROR';
    const validationErrors = errData?.validationErrors;
    throw new ApiError(response.status, message, code, validationErrors);
  }

  // The backend wraps successful responses in ApiResponse<T>
  const apiResponse = data as ApiResponse<T>;
  if (apiResponse && typeof apiResponse === 'object' && 'success' in apiResponse) {
    return apiResponse.data;
  }

  return data as T;
}

export const api = {
  get<T>(endpoint: string, options?: RequestOptions): Promise<T> {
    return request<T>(endpoint, { ...options, method: 'GET' });
  },

  post<T>(endpoint: string, body?: unknown, options?: RequestOptions): Promise<T> {
    return request<T>(endpoint, {
      ...options,
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined,
    });
  },

  put<T>(endpoint: string, body?: unknown, options?: RequestOptions): Promise<T> {
    return request<T>(endpoint, {
      ...options,
      method: 'PUT',
      body: body ? JSON.stringify(body) : undefined,
    });
  },

  patch<T>(endpoint: string, body?: unknown, options?: RequestOptions): Promise<T> {
    return request<T>(endpoint, {
      ...options,
      method: 'PATCH',
      body: body ? JSON.stringify(body) : undefined,
    });
  },

  delete<T>(endpoint: string, options?: RequestOptions): Promise<T> {
    return request<T>(endpoint, { ...options, method: 'DELETE' });
  },
};
