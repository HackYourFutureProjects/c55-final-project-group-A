import type {
  AdminEventDetail,
  AdminEventPage,
  CreateEventRequest,
} from "@/types/admin";
import type { LoginRequest, RegisterRequest } from "@/types/auth";
import type { Comment, CommentPage, CommentRequest } from "@/types/comment";
import type {
  Category,
  Event,
  EventDetail,
  EventFilters,
  EventPage,
} from "@/types/event";
import type {
  Feedback,
  FeedbackPage,
  PostFeedbackRequest,
} from "@/types/feedback";
import type { LocationSuggestion } from "@/types/location";
import type { UpdateUserRequest, User } from "@/types/user";
import type { Weather } from "@/types/weather";

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

  if (filters.sort) {
    query.set("sort", filters.sort);
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
  page = 0,
  size = 9,
  cookieHeader?: string,
): Promise<EventPage> {
  const res = await fetch(
    apiUrl(`/api/users/me/saved?page=${page}&size=${size}`),
    {
      credentials: "include",
      headers: cookieHeader ? { Cookie: cookieHeader } : undefined,
    },
  );
  if (!res.ok) {
    throw new Error(`Failed to fetch saved events: ${res.status}`);
  }
  return res.json();
}

export async function getGoingEvents(
  page = 0,
  size = 9,
  cookieHeader?: string,
): Promise<EventPage> {
  const res = await fetch(
    apiUrl(`/api/users/me/going?page=${page}&size=${size}`),
    {
      credentials: "include",
      headers: cookieHeader ? { Cookie: cookieHeader } : undefined,
    },
  );
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

// Publish, cancel and delete
export async function publishAdminEvent(eventId: string): Promise<void> {
  const res = await fetch(apiUrl(`/api/admin/events/${eventId}/publish`), {
    method: "PATCH",
    credentials: "include",
  });
  if (!res.ok) {
    const problem = await res.json().catch(() => null);
    throw new Error(
      problem?.detail ?? `Failed to publish event: ${res.status}`,
    );
  }
}

export async function cancelAdminEvent(eventId: string): Promise<void> {
  const res = await fetch(apiUrl(`/api/admin/events/${eventId}/cancel`), {
    method: "PATCH",
    credentials: "include",
  });
  if (!res.ok) {
    const problem = await res.json().catch(() => null);
    throw new Error(problem?.detail ?? `Failed to cancel event: ${res.status}`);
  }
}

export async function deleteAdminEvent(eventId: string): Promise<void> {
  const res = await fetch(apiUrl(`/api/admin/events/${eventId}`), {
    method: "DELETE",
    credentials: "include",
  });
  if (!res.ok) {
    const problem = await res.json().catch(() => null);
    throw new Error(problem?.detail ?? `Failed to delete event: ${res.status}`);
  }
}

export async function updateAdminEvent(
  eventId: string,
  event: CreateEventRequest,
  image: File | null,
): Promise<AdminEventDetail> {
  const formData = new FormData();

  formData.append(
    "event",
    new Blob([JSON.stringify(event)], { type: "application/json" }),
  );
  if (image) {
    formData.append("image", image);
  }

  const res = await fetch(apiUrl(`/api/admin/events/${eventId}`), {
    method: "PATCH",
    credentials: "include",
    body: formData,
  });

  if (!res.ok) {
    const problem = await res.json().catch(() => null);
    throw new Error(problem?.detail ?? `Failed to update event: ${res.status}`);
  }

  return res.json();
}

// --- Comments ---

export async function getEventComments(eventId: string): Promise<CommentPage> {
  const res = await fetch(apiUrl(`/api/events/${eventId}/comments?size=100`), {
    credentials: "include",
  });
  if (!res.ok) {
    const problem = await res.json().catch(() => null);
    throw new Error(
      problem?.detail ?? `Failed to load comments: ${res.status}`,
    );
  }
  return res.json();
}

export async function createComment(
  eventId: string,
  data: CommentRequest,
): Promise<Comment> {
  const res = await fetch(apiUrl(`/api/events/${eventId}/comments`), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const problem = await res.json().catch(() => null);
    throw new Error(problem?.detail ?? `Failed to post comment: ${res.status}`);
  }
  return res.json();
}

export async function updateComment(
  commentId: string,
  data: CommentRequest,
): Promise<Comment> {
  const res = await fetch(apiUrl(`/api/comments/${commentId}`), {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const problem = await res.json().catch(() => null);
    throw new Error(
      problem?.detail ?? `Failed to update comment: ${res.status}`,
    );
  }
  return res.json();
}

// 204 No Content, so there is nothing to parse
export async function deleteComment(commentId: string): Promise<void> {
  const res = await fetch(apiUrl(`/api/comments/${commentId}`), {
    method: "DELETE",
    credentials: "include",
  });
  if (!res.ok) {
    const problem = await res.json().catch(() => null);
    throw new Error(
      problem?.detail ?? `Failed to delete comment: ${res.status}`,
    );
  }
}

// --- Admin comment actions ---

export async function deleteCommentAsAdmin(commentId: string): Promise<void> {
  const res = await fetch(apiUrl(`/api/admin/comments/${commentId}`), {
    method: "DELETE",
    credentials: "include",
  });
  if (!res.ok) {
    const problem = await res.json().catch(() => null);
    throw new Error(
      problem?.detail ?? `Failed to delete comment: ${res.status}`,
    );
  }
}

export async function createAdminReply(
  commentId: string,
  data: CommentRequest,
): Promise<Comment> {
  const res = await fetch(apiUrl(`/api/admin/comments/${commentId}/reply`), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const problem = await res.json().catch(() => null);
    throw new Error(problem?.detail ?? `Failed to post reply: ${res.status}`);
  }
  return res.json();
}

export async function updateAdminReply(
  commentId: string,
  data: CommentRequest,
): Promise<Comment> {
  const res = await fetch(apiUrl(`/api/admin/comments/${commentId}/reply`), {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const problem = await res.json().catch(() => null);
    throw new Error(problem?.detail ?? `Failed to update reply: ${res.status}`);
  }
  return res.json();
}

export async function deleteAdminReply(commentId: string): Promise<void> {
  const res = await fetch(apiUrl(`/api/admin/comments/${commentId}/reply`), {
    method: "DELETE",
    credentials: "include",
  });
  if (!res.ok) {
    const problem = await res.json().catch(() => null);
    throw new Error(problem?.detail ?? `Failed to delete reply: ${res.status}`);
  }
}

export async function getSimilarEvents(eventId: string): Promise<Event[]> {
  const res = await fetch(apiUrl(`/api/events/${eventId}/similar`));
  if (!res.ok) {
    const problem = await res.json().catch(() => null);
    throw new Error(
      problem?.detail ?? `Failed to load similar events: ${res.status}`,
    );
  }
  return res.json();
}

//-----Feedback------

export async function submitFeedback(data: PostFeedbackRequest): Promise<void> {
  const res = await fetch(apiUrl("/api/feedback"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    throw new Error("Failed to submit feedback");
  }
}

export async function getAdminFeedback(
  page = 0,
  size = 9,
): Promise<FeedbackPage> {
  const res = await fetch(
    apiUrl(`/api/admin/feedback?page=${page}&size=${size}`),
    {
      credentials: "include",
    },
  );

  if (!res.ok) {
    throw new Error("Failed to load feedback");
  }

  return res.json();
}

export async function updateFeedbackReviewed(
  id: string,
  isReviewed: boolean,
): Promise<Feedback> {
  const res = await fetch(apiUrl(`/api/admin/feedback/${id}`), {
    method: "PATCH",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ isReviewed }),
  });

  if (!res.ok) {
    throw new Error("Failed to update feedback");
  }

  return res.json();
}

export async function getWeather(
  latitude: number,
  longitude: number,
  time: string,
): Promise<Weather> {
  const res = await fetch(
    apiUrl(
      `/api/weather?latitude=${latitude}&longitude=${longitude}&time=${time}`,
    ),
  );

  if (!res.ok) {
    throw new Error("Failed to load weather");
  }

  return res.json();
}
