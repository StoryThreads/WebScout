import { apiClient } from './client';
import {
  LoginRequest,
  LoginResponse,
  RegisterUserRequest,
  RefreshTokenRequest,
  RefreshTokenResponse,
  LogoutRequest,
  UserResponse,
} from '../types/auth';

export const authApi = {
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/auth/login', credentials);
    return response.data;
  },

  register: async (payload: RegisterUserRequest): Promise<UserResponse> => {
    const response = await apiClient.post<UserResponse>('/auth/register', payload);
    return response.data;
  },

  refresh: async (payload: RefreshTokenRequest): Promise<RefreshTokenResponse> => {
    const response = await apiClient.post<RefreshTokenResponse>('/auth/refresh', payload);
    return response.data;
  },

  logout: async (payload: LogoutRequest): Promise<void> => {
    await apiClient.post('/auth/logout', payload);
  },

  getCurrentUser: async (): Promise<UserResponse> => {
    const response = await apiClient.get<UserResponse>('/users/me');
    return response.data;
  },
};
