'use client';

import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { UserResponse, LoginRequest, RegisterRequest, AuthResponse } from '@/types';
import { api } from '@/lib/api';
import {
  getStoredToken,
  setStoredToken,
  getStoredUser,
  setStoredUser,
  clearStoredAuth,
} from '@/lib/auth';
import { ROLE_DASHBOARDS } from '@/lib/constants';

interface AuthContextType {
  user: UserResponse | null;
  token: string | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (data: LoginRequest) => Promise<UserResponse>;
  register: (data: Omit<RegisterRequest, 'role'>) => Promise<UserResponse>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<UserResponse | null>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  const refreshUser = useCallback(async (): Promise<UserResponse | null> => {
    try {
      const freshUser = await api.get<UserResponse>('/auth/me');
      setUser(freshUser);
      setStoredUser(freshUser);
      return freshUser;
    } catch {
      clearStoredAuth();
      setUser(null);
      setToken(null);
      return null;
    }
  }, []);

  useEffect(() => {
    const savedToken = getStoredToken();
    const savedUser = getStoredUser();

    if (savedToken) {
      setToken(savedToken);
      if (savedUser) {
        setUser(savedUser);
      }
      refreshUser().finally(() => {
        setIsLoading(false);
      });
    } else {
      setIsLoading(false);
    }
  }, [refreshUser]);

  const login = async (credentials: LoginRequest): Promise<UserResponse> => {
    setIsLoading(true);
    try {
      const response = await api.post<AuthResponse>('/auth/login', credentials);
      setStoredToken(response.token);
      setStoredUser(response.user);
      setToken(response.token);
      setUser(response.user);

      const redirectPath = ROLE_DASHBOARDS[response.user.role] || '/customer/dashboard';
      router.push(redirectPath);
      return response.user;
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (data: Omit<RegisterRequest, 'role'>): Promise<UserResponse> => {
    setIsLoading(true);
    try {
      // Public registration strictly creates CUSTOMER role
      const payload: RegisterRequest = {
        ...data,
        role: 'CUSTOMER',
      };
      const response = await api.post<AuthResponse>('/auth/register', payload);
      setStoredToken(response.token);
      setStoredUser(response.user);
      setToken(response.token);
      setUser(response.user);

      const redirectPath = ROLE_DASHBOARDS[response.user.role] || '/customer/dashboard';
      router.push(redirectPath);
      return response.user;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async (): Promise<void> => {
    try {
      await api.post('/auth/logout');
    } catch {
      // Ignore backend logout errors and proceed to clear client auth
    } finally {
      clearStoredAuth();
      setUser(null);
      setToken(null);
      router.push('/login');
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isLoading,
        isAuthenticated: !!token && !!user,
        login,
        register,
        logout,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
