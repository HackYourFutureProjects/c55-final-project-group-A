import type { LoginRequest, RegisterRequest } from "@/types/auth";
import type { EventDetail, EventFilters, EventPage } from "@/types/event";

import type { LocationSuggestion } from "@/types/location";

import type { UpdateUserRequest, User } from "@/types/user";

function apiUrl(path: string) {
  if (typeof window === "undefined") {
    const backendUrl = process.env.BACKEND_API_URL || "http://localhost:8080";
    return new URL(path, backendUrl).toString();
  }
  return path;
}

export async function getEvents(
  filters: EventFilters = {},
): Promise<EventPage> {
  // URLSearchParams builds the query string and escapes values for us
  const query = new URLSearchParams();

  query.set("page", String(filters.page ?? 0));
  query.set("size", String(filters.size ?? 9));

  if (filters.search) {
    query.set("search", filters.search);
  }

  // append (not set) so the same key repeats: ?categoryIds=a&categoryIds=b
  for (const categoryId of filters.categoryIds ?? []) {
    query.append("categoryIds", categoryId);
  }

  // The radius filter only works when all three are sent together
  if (filters.latitude && filters.longitude && filters.radiusKm) {
    query.set("latitude", String(filters.latitude));
    query.set("longitude", String(filters.longitude));
    query.set("radiusKm", String(filters.radiusKm));
  }

  const response = await fetch(apiUrl(`/api/events?${query.toString()}`));

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

export async function getCurrentUser(): Promise<User | null> {
  const response = await fetch(apiUrl("/api/users/me"), {
    credentials: "include",
  });

  if (!response.ok) {
    return null;
  }

  return response.json();
}

export async function getLocationSuggestions(
  query: string,
): Promise<LocationSuggestion[]> {
  const response = await fetch(
    apiUrl(`/api/locations/suggest?q=${encodeURIComponent(query)}`),
  );

  if (!response.ok) {
    throw new Error(`Failed to load suggestions: ${response.status}`);
  }

  return response.json();
}

export async function updateCurrentUser(
  data: UpdateUserRequest,
): Promise<User> {
  const response = await fetch(apiUrl("/api/users/me"), {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw new Error(`Failed to update profile: ${response.status}`);
  }

  return response.json();
}

export async function deleteCurrentUser(): Promise<void> {
  const response = await fetch(apiUrl("/api/users/me"), {
    method: "DELETE",
    credentials: "include",
  });

  if (!response.ok) {
    throw new Error(`Failed to delete account: ${response.status}`);
  }
}
