export interface RegisterUserRequest {
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface RefreshTokenResponse {
  accessToken: string;
  refreshToken: string;
}

export interface LogoutRequest {
  refreshToken: string;
}

export interface UserResponse {
  id: number;
  email: string;
  status: 'ACTIVE' | 'DISABLED';
}

export interface ApiErrorResponse {
  code: string;
  message: string;
  timestamp: string;
  path: string;
}
