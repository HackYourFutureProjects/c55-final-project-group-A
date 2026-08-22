"use client";

import { createContext, useContext, useEffect, useState } from "react";
import { getCurrentUser } from "@/lib/api";
import type { User } from "@/types/user";

// What the context holds: who is logged in, whether we're still
// checking, and a way to re-read the user from the backend
interface AuthContextValue {
  user: User | null;
  isLoading: boolean;
  refresh: () => Promise<User | null>;
}

// Creates the context. null is the default value, used when no
// provider has supplied one yet
const AuthContext = createContext<AuthContextValue | null>(null);

// Wrapper component: asks the backend who is logged in once,
// then shares the result with every component inside it
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Re-reads the current user. Called after login, logout,
  // and profile edits so the UI stays in sync
  const refresh = async () => {
    const currentUser = await getCurrentUser();
    setUser(currentUser);
    return currentUser;
  };

  // Empty dependency array = run once on mount.
  // finally clears the loading flag whether a user was found or not
  useEffect(() => {
    getCurrentUser()
      .then(setUser)
      .finally(() => setIsLoading(false));
  }, []);
  return (
    <AuthContext.Provider value={{ user, isLoading, refresh }}>
      {children}
    </AuthContext.Provider>
  );
}

// Shortcut for consumers: instead of useContext plus a null check
// in every component, they just call useAuth().
// Throws if used outside AuthProvider, which is a bug worth failing on
export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }

  return context;
}
