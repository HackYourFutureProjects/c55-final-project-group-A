import { notFound } from "next/navigation";
import { getEventById } from "@/lib/api";
import type { EventStatus } from "@/types/event";

interface EventDetailPageProps {
  params: Promise<{ id: string }>;
}

function formatDateRange(startAt: string, endAt: string) {
  const start = new Date(startAt);
  const end = new Date(endAt);

  const date = start.toLocaleDateString("en-GB", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });

  const startTime = start.toLocaleTimeString("en-GB", {
    hour: "2-digit",
    minute: "2-digit",
  });

  const endTime = end.toLocaleTimeString("en-GB", {
    hour: "2-digit",
    minute: "2-digit",
  });

  return `${date} · ${startTime} – ${endTime}`;
}

function getStatusMessage(eventStatus: EventStatus) {
  if (eventStatus === "CANCELLED") {
    return "This event has been cancelled";
  }
  if (eventStatus === "PAST") {
    return "This event has ended";
  }
  if (eventStatus === "ONGOING") {
    return "Happening now";
  }
  return null;
}

export default async function EventDetailPage({
  params,
}: EventDetailPageProps) {
  const { id } = await params;

  const event = await getEventById(id).catch(() => null);

  if (!event) {
    notFound();
  }
  const statusMessage = getStatusMessage(event.eventStatus);

  return (
    <main>
      <div>
        {event.imageUrl ? (
          <img src={event.imageUrl} alt={event.title} />
        ) : (
          <p>{event.title}</p>
        )}
      </div>

      <div>
        {event.categories.map((category) => (
          <span key={category.id}>{category.name}</span>
        ))}
      </div>

      {statusMessage && <p>{statusMessage}</p>}

      <h1>{event.title}</h1>

      <p>{formatDateRange(event.startAt, event.endAt)}</p>

      <p>
        {event.street} {event.houseNumber}, {event.postalCode} {event.cityName}
      </p>

      {event.description && <p>{event.description}</p>}

      <hr />

      <div>
        <span>{event.price === 0 ? "Free" : `€${event.price}`}</span>
        <span>{event.goingCount} going</span>
      </div>
    </main>
  );
}
