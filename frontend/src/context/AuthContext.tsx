import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { UserResponse, LoginRequest, RegisterUserRequest } from '../types/auth';
import { authApi } from '../api/auth';
import { TOKEN_STORAGE } from '../api/client';

interface AuthContextType {
  user: UserResponse | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (payload: RegisterUserRequest) => Promise<void>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const refreshProfile = useCallback(async () => {
    try {
      const profile = await authApi.getCurrentUser();
      setUser(profile);
    } catch {
      setUser(null);
      TOKEN_STORAGE.clearTokens();
    }
  }, []);

  // Hydrate auth state on startup
  useEffect(() => {
    const initAuth = async () => {
      const token = TOKEN_STORAGE.getAccessToken();
      if (token) {
        await refreshProfile();
      }
      setIsLoading(false);
    };

    initAuth();

    // Listen for session expiry from API interceptor
    const handleSessionExpired = () => {
      setUser(null);
    };

    window.addEventListener('webscout_session_expired', handleSessionExpired);
    return () => {
      window.removeEventListener('webscout_session_expired', handleSessionExpired);
    };
  }, [refreshProfile]);

  const login = async (credentials: LoginRequest) => {
    const response = await authApi.login(credentials);
    TOKEN_STORAGE.setTokens(response.accessToken, response.refreshToken);
    const profile = await authApi.getCurrentUser();
    setUser(profile);
  };

  const register = async (payload: RegisterUserRequest) => {
    await authApi.register(payload);
    // After registration, automatically login the user
    await login({ email: payload.email, password: payload.password });
  };

  const logout = async () => {
    const refreshToken = TOKEN_STORAGE.getRefreshToken();
    try {
      if (refreshToken) {
        await authApi.logout({ refreshToken });
      }
    } catch {
      // Ignore network errors on logout
    } finally {
      TOKEN_STORAGE.clearTokens();
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        logout,
        refreshProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
