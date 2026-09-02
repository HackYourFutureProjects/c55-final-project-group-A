import Link from "next/link";
import {
  getAdminEventStatus,
  getAdminEventStatusClasses,
} from "@/lib/adminEventStatus";
import type { AdminEventSummary } from "@/types/admin";

interface AdminEventCardProps {
  event: AdminEventSummary;
  onPublish: (eventId: string) => void;
  onCancel: (eventId: string) => void;
  onDelete: (eventId: string) => void;
  isBusy: boolean;
}

// Actions are quiet text links so the rows stay calm when repeated
const ACTION =
  "font-semibold text-neutral-600 text-sm hover:text-neutral-900 hover:underline disabled:opacity-50";

export default function AdminEventCard({
  event,
  onPublish,
  onCancel,
  onDelete,
  isBusy,
}: AdminEventCardProps) {
  const status = getAdminEventStatus(event);

  // Cancelled and ended events cannot be changed any more
  const canEdit = status === "DRAFT" || status === "PUBLISHED";

  return (
    <li className="flex items-center justify-between gap-6 rounded-2xl border border-neutral-200 bg-white px-5 py-4">
      <div className="min-w-0">
        <div className="flex items-center gap-3">
          <h3 className="truncate font-semibold">{event.title}</h3>
          <span
            className={`shrink-0 rounded-full px-2 py-0.5 font-semibold text-xs ${getAdminEventStatusClasses(status)}`}
          >
            {status}
          </span>
        </div>
        <p className="mt-1 text-neutral-500 text-sm">
          {new Date(event.startAt).toLocaleString("en-GB", {
            timeZone: "Europe/Amsterdam",
            day: "2-digit",
            month: "short",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
          })}{" "}
          · {event.cityName}
        </p>
      </div>

      <div className="flex shrink-0 items-center gap-5">
        {canEdit && (
          <Link href={`/admin/events/${event.id}/edit`} className={ACTION}>
            Edit
          </Link>
        )}

        {status === "DRAFT" && (
          <button
            type="button"
            disabled={isBusy}
            onClick={() => onPublish(event.id)}
            className={ACTION}
          >
            Publish
          </button>
        )}

        {status === "PUBLISHED" && (
          <button
            type="button"
            disabled={isBusy}
            onClick={() => onCancel(event.id)}
            className={ACTION}
          >
            Cancel
          </button>
        )}

        {status === "DRAFT" && (
          <button
            type="button"
            disabled={isBusy}
            onClick={() => onDelete(event.id)}
            className="font-semibold text-red-600 text-sm hover:text-red-700 hover:underline disabled:opacity-50"
          >
            Delete
          </button>
        )}
      </div>
    </li>
  );
}
