// Edit an existing event using the same form as creating one.

"use client";

import { useRouter } from "next/navigation";
import { use, useEffect, useState } from "react";
import EventForm from "@/components/admin/EventForm";
import ProtectedRoute from "@/components/auth/ProtectedRoute";
import { getAdminEventById, updateAdminEvent } from "@/lib/api";
import type { AdminEventDetail, CreateEventRequest } from "@/types/admin";

export default function EditEventPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);

  return (
    <ProtectedRoute adminOnly>
      <EditEventContent eventId={id} />
    </ProtectedRoute>
  );
}

function EditEventContent({ eventId }: { eventId: string }) {
  const router = useRouter();
  const [event, setEvent] = useState<AdminEventDetail | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    getAdminEventById(eventId)
      .then(setEvent)
      .catch(() => setError("Could not load this event"));
  }, [eventId]);

  async function handleUpdate(updated: CreateEventRequest, image: File | null) {
    await updateAdminEvent(eventId, updated, image);
    router.push("/admin/events");
  }

  if (error)
    return (
      <div className="mx-auto max-w-7xl px-6 py-8">
        <p className="rounded-lg bg-red-50 px-4 py-3 font-semibold text-red-700 text-sm">
          {error}
        </p>
      </div>
    );

  if (!event)
    return (
      <div className="mx-auto max-w-7xl px-6 py-8">
        <p className="text-neutral-500 text-sm">Loading...</p>
      </div>
    );

  return (
    <div className="mx-auto max-w-3xl px-6 py-8">
      <h1 className="font-semibold text-3xl">Edit event</h1>
      <p className="mt-1 text-neutral-500 text-sm">{event.title}</p>

      <div className="mt-8 rounded-2xl border border-neutral-200 bg-white p-6">
        <EventForm initialEvent={event} onSubmit={handleUpdate} />
      </div>
    </div>
  );
}
