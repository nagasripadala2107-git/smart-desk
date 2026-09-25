import { UserResponse, UserRole } from '@/types';

const TOKEN_KEY = 'smartdesk_token';
const USER_KEY = 'smartdesk_user';

export function getStoredToken(): string | null {
  if (typeof window === 'undefined') return null;
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setStoredToken(token: string): void {
  if (typeof window === 'undefined') return;
  try {
    localStorage.setItem(TOKEN_KEY, token);
  } catch (e) {
    console.error('Failed to save token to storage', e);
  }
}

export function removeStoredToken(): void {
  if (typeof window === 'undefined') return;
  try {
    localStorage.removeItem(TOKEN_KEY);
  } catch (e) {
    console.error('Failed to remove token from storage', e);
  }
}

export function getStoredUser(): UserResponse | null {
  if (typeof window === 'undefined') return null;
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function setStoredUser(user: UserResponse): void {
  if (typeof window === 'undefined') return;
  try {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  } catch (e) {
    console.error('Failed to save user to storage', e);
  }
}

export function clearStoredAuth(): void {
  if (typeof window === 'undefined') return;
  try {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  } catch (e) {
    console.error('Failed to clear auth storage', e);
  }
}

export function hasRole(user: UserResponse | null, allowedRoles: UserRole[]): boolean {
  if (!user || !user.role) return false;
  return allowedRoles.includes(user.role);
}
