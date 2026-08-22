import type { LoginRequest, RegisterRequest } from "@/types/auth";
import type { EventDetail, EventPage } from "@/types/event";

function apiUrl(path: string) {
  if (typeof window === "undefined") {
    const backendUrl = process.env.BACKEND_API_URL || "http://localhost:8080";
    return new URL(path, backendUrl).toString();
  }
  return path;
}

export async function getEvents(page = 0, size = 9): Promise<EventPage> {
  const response = await fetch(apiUrl(`/api/events?page=${page}&size=${size}`));
  if (!response.ok) {
    throw new Error(`Failed to load events: ${response.status}`);
  }
  return response.json();
}

export async function getEventById(id: string): Promise<EventDetail> {
  const response = await fetch(apiUrl(`/api/events/${id}`), {
    credentials: "include",
  });

  if (!response.ok) {
    throw new Error("Failed to fetch event");
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
