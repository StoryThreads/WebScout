import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { RefreshTokenResponse } from '../types/auth';

const API_BASE_URL = '/api/v1';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const TOKEN_STORAGE = {
  getAccessToken: () => localStorage.getItem('webscout_access_token'),
  getRefreshToken: () => localStorage.getItem('webscout_refresh_token'),
  setTokens: (accessToken: string, refreshToken: string) => {
    localStorage.setItem('webscout_access_token', accessToken);
    localStorage.setItem('webscout_refresh_token', refreshToken);
  },
  clearTokens: () => {
    localStorage.removeItem('webscout_access_token');
    localStorage.removeItem('webscout_refresh_token');
  },
};

// Queue for pending requests while a refresh token exchange is underway
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else if (token) {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Attach Authorization header if access token exists
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = TOKEN_STORAGE.getAccessToken();
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor with automatic token rotation & reuse prevention
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // If no response or not a 401, bubble error up
    if (!error.response || error.response.status !== 401) {
      return Promise.reject(error);
    }

    // Skip retry on auth endpoints to avoid loops
    const requestUrl = originalRequest.url || '';
    if (
      requestUrl.includes('/auth/login') ||
      requestUrl.includes('/auth/register') ||
      requestUrl.includes('/auth/refresh')
    ) {
      return Promise.reject(error);
    }

    if (originalRequest._retry) {
      return Promise.reject(error);
    }

    const currentRefreshToken = TOKEN_STORAGE.getRefreshToken();
    if (!currentRefreshToken) {
      TOKEN_STORAGE.clearTokens();
      window.dispatchEvent(new CustomEvent('webscout_session_expired'));
      return Promise.reject(error);
    }

    if (isRefreshing) {
      // If refresh is already in progress, enqueue this request to wait
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      })
        .then((token) => {
          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${token}`;
          }
          return apiClient(originalRequest);
        })
        .catch((err) => Promise.reject(err));
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      // Call refresh endpoint with raw axios to bypass interceptors
      const { data } = await axios.post<RefreshTokenResponse>(
        `${API_BASE_URL}/auth/refresh`,
        { refreshToken: currentRefreshToken }
      );

      TOKEN_STORAGE.setTokens(data.accessToken, data.refreshToken);
      processQueue(null, data.accessToken);

      if (originalRequest.headers) {
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
      }

      return apiClient(originalRequest);
    } catch (refreshError) {
      processQueue(refreshError, null);
      TOKEN_STORAGE.clearTokens();
      window.dispatchEvent(new CustomEvent('webscout_session_expired'));
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);
