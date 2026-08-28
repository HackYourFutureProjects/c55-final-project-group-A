// Works out which status label to show for an admin event.
// The backend sends two booleans, not a single status field.

import type { AdminEventStatus } from "@/types/admin";

export function getAdminEventStatus(event: {
  isPublished: boolean;
  cancelled: boolean;
  endAt: string;
}): AdminEventStatus {
  if (event.cancelled) return "CANCELLED";
  if (!event.isPublished) return "DRAFT";
  if (new Date(event.endAt) < new Date()) return "ENDED";
  return "PUBLISHED";
}

// Badge colours for each admin event status
const STATUS_CLASSES: Record<string, string> = {
  DRAFT: "bg-neutral-100 text-neutral-700",
  PUBLISHED: "bg-neutral-900 text-white",
  CANCELLED: "bg-red-50 text-red-700",
  ENDED: "bg-neutral-100 text-neutral-400",
};

export function getAdminEventStatusClasses(status: string) {
  return STATUS_CLASSES[status] ?? "bg-neutral-100 text-neutral-700";
}
