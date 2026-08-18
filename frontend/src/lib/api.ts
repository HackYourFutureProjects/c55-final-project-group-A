import type { LoginRequest, RegisterRequest } from "@/types/auth";
import type { EventDetail, EventPage } from "@/types/event";

export async function getEvents(page = 0, size = 9): Promise<EventPage> {
  const response = await fetch(`/api/events?page=${page}&size=${size}`);
  if (!response.ok) {
    throw new Error(`Failed to load events: ${response.status}`);
  }
  return response.json();
}

export async function register(data: RegisterRequest): Promise<void> {
  const response = await fetch("/api/auth/register", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
    credentials: "include",
  });
  if (!response.ok) {
    throw new Error(`Failed to register: ${response.status}`);
  }
}

export async function login(data: LoginRequest): Promise<void> {
  const response = await fetch("/api/auth/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
    credentials: "include",
  });
  if (!response.ok) {
    throw new Error(`Failed to login: ${response.status}`);
  }
}

export async function logout(): Promise<void> {
  const response = await fetch("/api/auth/logout", {
    method: "DELETE",
    credentials: "include",
  });
  if (!response.ok) {
    throw new Error(`Failed to logout: ${response.status}`);
  }
}
