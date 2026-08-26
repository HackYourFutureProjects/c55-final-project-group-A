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
