import type { Event } from "@/types/event";

export async function getEvents(): Promise<Event[]> {
  const response = await fetch("/api/events");
  if (!response.ok) {
    throw new Error(`Failed to load events: ${response.status}`);
  }
  return response.json();
}
