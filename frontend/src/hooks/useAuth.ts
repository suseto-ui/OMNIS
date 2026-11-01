import { useState, useEffect, useCallback } from 'react';

export interface User {
  username: string;
  email: string;
  role: string;
}

export interface AuthState {
  token: string | null;
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

const BASE_URL = "https://ais-dev-ex6yxewfd4hwymxojrmgxr-291037164760.europe-west3.run.app";

export function useAuth() {
  const [state, setState] = useState<AuthState>({
    token: localStorage.getItem('omnis_token'),
    user: null,
    isAuthenticated: !!localStorage.getItem('omnis_token'),
    isLoading: true,
  });

  const fetchProfile = useCallback(async (token: string) => {
    try {
      const res = await fetch(`${BASE_URL}/api/user/profile`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      if (res.ok) {
        const profile = await res.json();
        setState(prev => ({
          ...prev,
          user: {
            username: profile.username,
            email: profile.email,
            role: profile.role
          },
          isAuthenticated: true,
          isLoading: false
        }));
      } else {
        logout();
      }
    } catch (e) {
      console.error("Profile fetch error", e);
      setState(prev => ({ ...prev, isLoading: false }));
    }
  }, []);

  useEffect(() => {
    if (state.token) {
      fetchProfile(state.token);
    } else {
      setState(prev => ({ ...prev, isLoading: false }));
    }
  }, [state.token, fetchProfile]);

  const login = async (username: string, password: string): Promise<{ success: boolean; error?: string }> => {
    try {
      const res = await fetch(`${BASE_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      if (res.ok) {
        const data = await res.json();
        localStorage.setItem('omnis_token', data.access_token);
        setState({
          token: data.access_token,
          user: { username: data.username, email: '', role: data.role },
          isAuthenticated: true,
          isLoading: false
        });
        return { success: true };
      } else {
        const err = await res.json();
        return { success: false, error: err.detail || "Neplatné údaje" };
      }
    } catch (e) {
      return { success: false, error: "Služba nedostupná" };
    }
  };

  const register = async (username: string, email: string, password: string): Promise<{ success: boolean; error?: string }> => {
    try {
      const res = await fetch(`${BASE_URL}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, email, password })
      });
      if (res.ok) {
        const data = await res.json();
        localStorage.setItem('omnis_token', data.access_token);
        setState({
          token: data.access_token,
          user: { username: data.username, email, role: data.role },
          isAuthenticated: true,
          isLoading: false
        });
        return { success: true };
      } else {
        const err = await res.json();
        return { success: false, error: err.detail || "Registrace selhala" };
      }
    } catch (e) {
      return { success: false, error: "Chyba sítě" };
    }
  };

  const loginWithGoogle = async (idToken: string): Promise<{ success: boolean; error?: string }> => {
    try {
      const res = await fetch(`${BASE_URL}/api/auth/google`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id_token: idToken })
      });
      if (res.ok) {
        const data = await res.json();
        localStorage.setItem('omnis_token', data.access_token);
        setState({
          token: data.access_token,
          user: { username: data.username, email: '', role: data.role },
          isAuthenticated: true,
          isLoading: false
        });
        return { success: true };
      } else {
        return { success: false, error: "Google login selhal" };
      }
    } catch (e) {
      return { success: false, error: "Chyba sítě" };
    }
  };

  const logout = () => {
    localStorage.removeItem('omnis_token');
    setState({
      token: null,
      user: null,
      isAuthenticated: false,
      isLoading: false
    });
  };

  return {
    ...state,
    login,
    register,
    loginWithGoogle,
    logout,
    refreshProfile: () => state.token && fetchProfile(state.token)
  };
}
