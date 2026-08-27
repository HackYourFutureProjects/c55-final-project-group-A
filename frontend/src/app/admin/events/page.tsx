// Admin dashboard: lists every event, including drafts and cancelled ones.

"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import ProtectedRoute from "@/components/auth/ProtectedRoute";
import Pagination from "@/components/Pagination";
import { getAdminEventStatus } from "@/lib/adminEventStatus";
import { getAdminEvents } from "@/lib/api";
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

  // Reload the list whenever the page number in the URL changes
  useEffect(() => {
    getAdminEvents(page)
      .then(setData)
      .catch(() => setError("Could not load events"));
  }, [page]);

  if (error) return <p>{error}</p>;
  if (!data) return <p>Loading...</p>;

  return (
    <div>
      <h1>Events</h1>
      <Link href="/admin">Create event</Link>

      <p>{data.totalElements} events</p>

      {data.events.map((event) => (
        <div key={event.id}>
          <h3>{event.title}</h3>
          <p>{new Date(event.startAt).toLocaleString("en-GB")}</p>
          <p>{event.cityName}</p>
          <p>{getAdminEventStatus(event)}</p>
        </div>
      ))}

      <Pagination
        page={data.page}
        totalPages={data.totalPages}
        hasNext={data.hasNext}
        basePath="/admin/events"
      />
    </div>
  );
}
