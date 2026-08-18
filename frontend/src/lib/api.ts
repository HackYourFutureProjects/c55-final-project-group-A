import type { EventDetail, EventPage } from "@/types/event";

export async function getEvents(page = 0, size = 9): Promise<EventPage> {
  const response = await fetch(`/api/events?page=${page}&size=${size}`);
  if (!response.ok) {
    throw new Error(`Failed to load events: ${response.status}`);
  }
  return response.json();
}

export async function getEventById(id: string): Promise<EventDetail> {
  const response = await fetch(`/api/events/${id}`);
  if (!response.ok) {
    throw new Error(`Failed to load event: ${response.status}`);
  }
  return response.json();
}
