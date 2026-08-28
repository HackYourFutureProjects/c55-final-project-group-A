// Admin dashboard: lists every event, including drafts and cancelled ones.

"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import AdminEventCard from "@/components/admin/AdminEventCard";
import ConfirmModal from "@/components/admin/ConfirmModal";
import ProtectedRoute from "@/components/auth/ProtectedRoute";
import Pagination from "@/components/Pagination";
import {
  cancelAdminEvent,
  deleteAdminEvent,
  getAdminEvents,
  publishAdminEvent,
} from "@/lib/api";
import type { AdminEventPage } from "@/types/admin";

export default function AdminEventsPage() {
  return (
    <ProtectedRoute adminOnly>
      <AdminEventsContent />
    </ProtectedRoute>
  );
}

function AdminEventsContent() {
  const searchParams = useSearchParams();
  const page = Number(searchParams.get("page")) || 0;

  const [data, setData] = useState<AdminEventPage | null>(null);
  const [error, setError] = useState("");
  const [isBusy, setIsBusy] = useState(false);

  // Which action is waiting for confirmation, if any
  const [confirming, setConfirming] = useState<{
    eventId: string;
    action: "cancel" | "delete";
  } | null>(null);

  // Wrapped in useCallback so it can be reused after every action
  const loadEvents = useCallback(() => {
    getAdminEvents(page)
      .then(setData)
      .catch(() => setError("Could not load events"));
  }, [page]);

  // Reload the list whenever the page number in the URL changes
  useEffect(loadEvents, [loadEvents]);

  // All three actions do the same thing around the request,
  // so one helper takes the api function and runs it.
  async function runAction(action: () => Promise<void>) {
    setError("");
    setIsBusy(true);
    try {
      await action();
      loadEvents();
    } catch (actionError) {
      setError(
        actionError instanceof Error
          ? actionError.message
          : "Something went wrong",
      );
    } finally {
      setIsBusy(false);
    }
  }

  if (!data && !error)
    return (
      <div className="mx-auto max-w-7xl px-6 py-8">
        <p className="text-neutral-500 text-sm">Loading...</p>
      </div>
    );

  return (
    <div className="mx-auto max-w-7xl px-6 py-8">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="font-semibold text-3xl">Events</h1>
          <p className="mt-1 text-neutral-500 text-sm">
            {data?.totalElements ?? 0} events in total
          </p>
        </div>
        <Link
          href="/admin"
          className="shrink-0 rounded-full bg-orange-600 px-5 py-2 font-semibold text-sm text-white hover:bg-orange-700"
        >
          Create event
        </Link>
      </div>

      {error && (
        <p className="mt-6 rounded-lg bg-red-50 px-4 py-3 font-semibold text-red-700 text-sm">
          {error}
        </p>
      )}

      {data && data.events.length === 0 && (
        <p className="mt-8 text-neutral-500 text-sm">No events yet.</p>
      )}

      {data && data.events.length > 0 && (
        <ul className="mt-8 space-y-3">
          {data.events.map((event) => (
            <AdminEventCard
              key={event.id}
              event={event}
              isBusy={isBusy}
              onPublish={(id) => runAction(() => publishAdminEvent(id))}
              onCancel={(id) =>
                setConfirming({ eventId: id, action: "cancel" })
              }
              onDelete={(id) =>
                setConfirming({ eventId: id, action: "delete" })
              }
            />
          ))}
        </ul>
      )}
      {data && (
        <div className="mt-8">
          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            hasNext={data.hasNext}
            basePath="/admin/events"
          />
        </div>
      )}

      {confirming && (
        <ConfirmModal
          title={
            confirming.action === "delete"
              ? "Delete this draft?"
              : "Cancel this event?"
          }
          message={
            confirming.action === "delete"
              ? "The draft will be removed permanently."
              : "People who saved it will see that it was cancelled. This cannot be undone."
          }
          confirmLabel={
            confirming.action === "delete" ? "Delete" : "Cancel event"
          }
          isBusy={isBusy}
          onClose={() => setConfirming(null)}
          onConfirm={async () => {
            await runAction(() =>
              confirming.action === "delete"
                ? deleteAdminEvent(confirming.eventId)
                : cancelAdminEvent(confirming.eventId),
            );
            setConfirming(null);
          }}
        />
      )}
    </div>
  );
}
