// Admin home: create an event on the left, the latest events on the right.

"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import EventForm from "@/components/admin/EventForm";
import ProtectedRoute from "@/components/auth/ProtectedRoute";
import { getAdminEventStatus } from "@/lib/adminEventStatus";
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
    getAdminEvents(0, 5)
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
    <div>
      <section>
        <h1>Create event</h1>
        <EventForm onSubmit={handleCreate} />
      </section>

      <section>
        <h2>Recent events</h2>
        {recent.map((event) => (
          <div key={event.id}>
            <h3>{event.title}</h3>
            <p>{new Date(event.startAt).toLocaleString("en-GB")}</p>
            <p>{getAdminEventStatus(event)}</p>
          </div>
        ))}
        <Link href="/admin/events">See all events</Link>
      </section>
    </div>
  );
}
