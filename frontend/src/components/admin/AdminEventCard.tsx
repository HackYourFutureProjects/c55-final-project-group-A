import { getAdminEventStatus } from "@/lib/adminEventStatus";
import type { AdminEventSummary } from "@/types/admin";

interface AdminEventCardProps {
  event: AdminEventSummary;
}

export default function AdminEventCard({ event }: AdminEventCardProps) {
  const status = getAdminEventStatus(event);

  return (
    <div>
      <h3>{event.title}</h3>
      <p>{new Date(event.startAt).toLocaleString("en-GB")}</p>
      <p>{event.cityName}</p>
      <p>{status}</p>
    </div>
  );
}
