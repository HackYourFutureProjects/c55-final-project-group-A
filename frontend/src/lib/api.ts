import type {
  AdminEventDetail,
  AdminEventPage,
  CreateEventRequest,
} from "@/types/admin";
import type { LoginRequest, RegisterRequest } from "@/types/auth";
import type {
  Category,
  EventDetail,
  EventFilters,
  EventPage,
} from "@/types/event";

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

  // Backend rejects a half-filled range, so send both dates or neither
  if (filters.dateFrom && filters.dateTo) {
    query.set("dateFrom", filters.dateFrom);
    query.set("dateTo", filters.dateTo);
  }

  // The radius filter only works when all three are sent together
  if (
    filters.latitude !== undefined &&
    filters.longitude !== undefined &&
    filters.radiusKm !== undefined
  ) {
    query.set("latitude", String(filters.latitude));
    query.set("longitude", String(filters.longitude));
    query.set("radiusKm", String(filters.radiusKm));
  }

  if (filters.price) {
    query.set("price", filters.price);
  }

  // Same repeated-key pattern as categoryIds
  for (const timeOfDay of filters.timesOfDay ?? []) {
    query.append("timesOfDay", timeOfDay);
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

export async function getCategories(): Promise<Category[]> {
  const response = await fetch(apiUrl("/api/categories"));

  if (!response.ok) {
    throw new Error(`Failed to load categories: ${response.status}`);
  }

  return response.json();
}

// --- Save / Going actions ---

export async function saveEvent(eventId: string): Promise<void> {
  const res = await fetch(apiUrl(`/api/events/${eventId}/saved`), {
    method: "POST",
    credentials: "include",
  });
  if (res.status !== 204) {
    throw new Error(`Failed to save event: ${res.status}`);
  }
}

export async function unsaveEvent(eventId: string): Promise<void> {
  const res = await fetch(apiUrl(`/api/events/${eventId}/saved`), {
    method: "DELETE",
    credentials: "include",
  });
  if (res.status !== 204) {
    throw new Error(`Failed to unsave event: ${res.status}`);
  }
}

export async function markGoing(eventId: string): Promise<void> {
  const res = await fetch(apiUrl(`/api/events/${eventId}/going`), {
    method: "POST",
    credentials: "include",
  });
  if (res.status !== 204) {
    throw new Error(`Failed to mark going: ${res.status}`);
  }
}

export async function unmarkGoing(eventId: string): Promise<void> {
  const res = await fetch(apiUrl(`/api/events/${eventId}/going`), {
    method: "DELETE",
    credentials: "include",
  });
  if (res.status !== 204) {
    throw new Error(`Failed to unmark going: ${res.status}`);
  }
}

// --- Saved / Going lists (for profile tabs + initial button state) ---

export async function getSavedEvents(
  cookieHeader?: string,
): Promise<EventPage> {
  const res = await fetch(apiUrl("/api/users/me/saved"), {
    credentials: "include",
    headers: cookieHeader ? { Cookie: cookieHeader } : undefined,
  });
  if (!res.ok) {
    throw new Error(`Failed to fetch saved events: ${res.status}`);
  }
  return res.json();
}

export async function getGoingEvents(
  cookieHeader?: string,
): Promise<EventPage> {
  const res = await fetch(apiUrl("/api/users/me/going"), {
    credentials: "include",
    headers: cookieHeader ? { Cookie: cookieHeader } : undefined,
  });
  if (!res.ok) {
    throw new Error(`Failed to fetch going events: ${res.status}`);
  }
  return res.json();
}

// --- Admin events (admin role only) ---

export async function getAdminEvents(
  page = 0,
  size = 10,
): Promise<AdminEventPage> {
  const res = await fetch(
    apiUrl(`/api/admin/events?page=${page}&size=${size}`),
    {
      credentials: "include",
    },
  );
  if (!res.ok) {
    throw new Error(`Failed to fetch admin events: ${res.status}`);
  }
  return res.json();
}

export async function getAdminEventById(
  eventId: string,
): Promise<AdminEventDetail> {
  const res = await fetch(apiUrl(`/api/admin/events/${eventId}`), {
    credentials: "include",
  });
  if (!res.ok) {
    throw new Error(`Failed to fetch admin event: ${res.status}`);
  }
  return res.json();
}

// Creates an event as a draft, or publishes it right away when publishNow is true.
// The request is multipart: a JSON part called "event" plus the image file.
export async function createAdminEvent(
  event: CreateEventRequest,
  image: File,
  publishNow: boolean,
): Promise<AdminEventDetail> {
  const formData = new FormData();

  // The backend requires this part to be application/json,
  // so we wrap the JSON in a Blob with that type. A plain string would give a 415.
  formData.append(
    "event",
    new Blob([JSON.stringify(event)], { type: "application/json" }),
  );
  formData.append("image", image);

  const res = await fetch(
    apiUrl(`/api/admin/events?publishNow=${publishNow}`),
    {
      method: "POST",
      credentials: "include",
      body: formData,
    },
  );

  if (!res.ok) {
    const problem = await res.json().catch(() => null);
    throw new Error(problem?.detail ?? `Failed to create event: ${res.status}`);
  }

  return res.json();
}
