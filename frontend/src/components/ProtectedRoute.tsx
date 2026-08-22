"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/context/AuthContext";

interface ProtectedRouteProps {
  children: React.ReactNode;
  adminOnly?: boolean;
}

export function ProtectedRoute({ children, adminOnly }: ProtectedRouteProps) {
  const { user, isLoading } = useAuth();
  const router = useRouter();

  const isAllowed = user && (!adminOnly || user.role === "admin");

  useEffect(() => {
    if (!isLoading && !isAllowed) {
      router.push("/login");
    }
  }, [isLoading, isAllowed, router]);

  if (isLoading || !isAllowed) {
    return null;
  }

  return <>{children}</>;
}
