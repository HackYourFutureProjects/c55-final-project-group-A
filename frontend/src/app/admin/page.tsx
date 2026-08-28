// Admin home: create an event on the left, the latest events on the right.

"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import EventForm from "@/components/admin/EventForm";
import ProtectedRoute from "@/components/auth/ProtectedRoute";
import {
  getAdminEventStatus,
  getAdminEventStatusClasses,
} from "@/lib/adminEventStatus";
import { createAdminEvent, getAdminEvents } from "@/lib/api";
import type { AdminEventSummary, CreateEventRequest } from "@/types/admin";

export default function AdminPage() {
  return (
    <ProtectedRoute adminOnly>
      <AdminContent />
    </ProtectedRoute>
  );
}

function AdminContent() {
  const router = useRouter();
  const [recent, setRecent] = useState<AdminEventSummary[]>([]);

  // Wrapped in useCallback so it can be reused after creating an event
  const loadRecent = useCallback(() => {
    getAdminEvents(0, 9)
      .then((data) => setRecent(data.events))
      .catch(() => setRecent([]));
  }, []);

  useEffect(loadRecent, [loadRecent]);

  async function handleCreate(
    event: CreateEventRequest,
    image: File | null,
    publishNow: boolean,
  ) {
    // The form only allows submitting without an image when editing
    if (!image) return;
    await createAdminEvent(event, image, publishNow);
    router.refresh();
    loadRecent();
  }

  return (
    <div className="mx-auto max-w-7xl px-6 py-8">
      <h1 className="font-semibold text-3xl">Create event</h1>
      <p className="mt-1 text-neutral-500 text-sm">
        Create a new event and review what was added recently.
      </p>

      <div className="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-3">
        <section className="rounded-2xl border border-neutral-200 bg-white p-6 lg:col-span-2">
          <h2 className="font-semibold text-xl">Create event</h2>
          <div className="mt-6">
            <EventForm onSubmit={handleCreate} />
          </div>
        </section>

        <section className="h-fit rounded-2xl border border-neutral-200 bg-white p-6 lg:sticky lg:top-8 lg:self-start">
          <h2 className="font-semibold text-xl">Recent events</h2>

          {recent.length === 0 ? (
            <p className="mt-4 text-neutral-500 text-sm">No events yet.</p>
          ) : (
            <ul className="mt-4 divide-y divide-neutral-100">
              {recent.map((event) => {
                const status = getAdminEventStatus(event);
                return (
                  <li key={event.id} className="py-4 first:pt-0 last:pb-0">
                    <h3 className="font-semibold text-sm">{event.title}</h3>
                    <div className="mt-2 flex items-center justify-between gap-3">
                      <p className="text-neutral-500 text-sm">
                        {new Date(event.startAt).toLocaleString("en-GB", {
                          day: "2-digit",
                          month: "short",
                          year: "numeric",
                          hour: "2-digit",
                          minute: "2-digit",
                        })}
                      </p>
                      <span
                        className={`shrink-0 rounded-full px-3 py-1 font-semibold text-xs ${getAdminEventStatusClasses(status)}`}
                      >
                        {status}
                      </span>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}

          <Link
            href="/admin/events"
            className="mt-6 inline-block rounded-full border border-neutral-200 px-5 py-2 font-semibold text-sm hover:bg-neutral-50"
          >
            See all events
          </Link>
        </section>
      </div>
    </div>
  );
}
