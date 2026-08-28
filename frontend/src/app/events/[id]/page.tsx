import { cookies } from "next/headers";
import { notFound } from "next/navigation";
import EventActions from "@/components/events/EventActions";
import EventMap from "@/components/events/EventMap";
import { getEventById, getGoingEvents, getSavedEvents } from "@/lib/api";
import type { EventStatus } from "@/types/event";
import { formatPrice } from "@/lib/formatPrice";

interface EventDetailPageProps {
  params: Promise<{ id: string }>;
}

function formatDateRange(startAt: string, endAt: string | null) {
  const start = new Date(startAt);

  const date = start.toLocaleDateString("en-GB", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });

  const startTime = start.toLocaleTimeString("en-GB", {
    hour: "2-digit",
    minute: "2-digit",
  });

  const endTime = endAt
    ? new Date(endAt).toLocaleTimeString("en-GB", {
        hour: "2-digit",
        minute: "2-digit",
      })
    : null;

  return { date, startTime, endTime };
}

function getStatusMessage(eventStatus: EventStatus) {
  if (eventStatus === "CANCELLED") return "This event has been cancelled";
  if (eventStatus === "PAST") return "This event has ended";
  if (eventStatus === "ONGOING") return "Happening now";
  return "Upcoming";
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
  const { date, startTime, endTime } = formatDateRange(
    event.startAt,
    event.endAt,
  );

  const cookieStore = await cookies();
  const sessionCookie = cookieStore.get("session_access_token");

  let initialIsSaved = false;
  let initialIsGoing = false;

  if (sessionCookie) {
    const cookieHeader = cookieStore.toString();
    const [savedPage, goingPage] = await Promise.all([
      getSavedEvents(0, 100, cookieHeader).catch(() => null),
      getGoingEvents(0, 100, cookieHeader).catch(() => null),
    ]);
    initialIsSaved = savedPage?.events.some((e) => e.id === id) ?? false;
    initialIsGoing = goingPage?.events.some((e) => e.id === id) ?? false;
  }

  return (
    <main className="relative mx-auto max-w-6xl overflow-hidden px-6 py-8">
      {/* Decorative blurred blobs behind the content — purely visual, no layout impact */}
      <div className="pointer-events-none absolute -left-32 top-40 h-112 w-150 rounded-full bg-purple-200 opacity-60 blur-3xl" />
      <div className="pointer-events-none absolute -right-32 top-96 h-72 w-172 rounded-full bg-orange-200 opacity-60 blur-3xl" />

      {/* z-10 keeps all real content above the decorative blobs */}
      <div className="relative z-10">
        {/* Breadcrumb navigation */}
        <p className="mb-4 text-sm text-gray-500">
          Home · {event.categories.map((c) => c.name).join(", ")} ·{" "}
          <span className="font-medium text-gray-700">{event.title}</span>
        </p>

        {/* Banner: fixed height so every event image takes up the same space */}
        <div className="relative h-64 w-full overflow-hidden rounded-2xl shadow-lg sm:h-80 lg:h-96">
          {event.imageUrl ? (
            <img
              src={event.imageUrl}
              alt={event.title}
              className="h-full w-full object-cover"
            />
          ) : (
            // Fallback gradient when there's no image
            <div className="h-full w-full bg-linear-to-br from-purple-200 via-gray-300 to-gray-800" />
          )}

          {/* Dark gradient overlay so white text stays readable on any photo */}
          <div className="absolute inset-0 bg-linear-to-t from-black/70 via-black/10 to-transparent" />

          {/* Text overlay: badges, title, date/location — sits on top of the image */}
          <div className="absolute bottom-0 left-0 w-full p-6 sm:p-8">
            <div className="mb-3 flex gap-2">
              {event.categories.map((category) => (
                <span
                  key={category.id}
                  className="rounded-full bg-white/90 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-purple-700"
                >
                  {category.name}
                </span>
              ))}
              {/* Status badge (Upcoming / Ongoing / Past / Cancelled) */}
              <span className="flex items-center gap-1 rounded-full bg-white/90 px-3 py-1 text-xs font-semibold text-green-700">
                <span className="h-1.5 w-1.5 rounded-full bg-green-500" />
                {statusMessage}
              </span>
            </div>

            <h1 className="text-3xl font-extrabold text-white sm:text-4xl">
              {event.title}
            </h1>

            <p className="mt-2 text-white/90">
              {date} · {startTime} · {event.street} {event.houseNumber},{" "}
              {event.cityName}
            </p>
          </div>
        </div>

        {/* Save / Going buttons, right below the banner */}
        <div className="mt-6">
          <EventActions
            eventId={event.id}
            initialIsSaved={initialIsSaved}
            initialIsGoing={initialIsGoing}
            initialGoingCount={event.goingCount}
          />
        </div>

        {/* Two-column layout: description on the left, info sidebar on the right */}
        <div className="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-3">
          {/* Left column: About this event */}
          <div className="self-start rounded-2xl border border-gray-100 bg-white p-6 shadow-sm lg:col-span-2">
            <h2 className="mb-4 text-xl font-bold text-gray-900">
              About this event
            </h2>
            {event.description ? (
              <p className="whitespace-pre-line text-gray-700">
                {event.description}
              </p>
            ) : (
              <p className="text-gray-400">No description provided.</p>
            )}
          </div>

          {/* Right column: info card — sticky so it stays visible while scrolling a long description */}
          <div className="relative overflow-hidden rounded-2xl bg-white p-6 shadow-sm lg:sticky lg:top-24 lg:self-start">
            {/* Accent stripe on the left edge, ties back to the badge colors */}
            <div className="absolute left-0 top-0 h-full w-1.5 bg-linear-to-b from-orange-500 to-purple-400" />

            <div className="pl-2">
              <p className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-gray-400">
                <span>📅</span> Date & time
              </p>
              <p className="mt-1 font-bold text-gray-900">
                {date} · {startTime}
              </p>
              <p className="text-sm text-gray-500">
                {endTime
                  ? `${startTime} – ${endTime}`
                  : `Starts at ${startTime}`}
              </p>

              <hr className="my-4" />

              <p className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-gray-400">
                <span>📍</span> Location
              </p>
              <p className="mt-1 font-bold text-gray-900">{event.cityName}</p>
              <p className="text-sm text-gray-500">
                {event.street} {event.houseNumber}, {event.postalCode}{" "}
                {event.cityName}
              </p>

              {/* Map pin at the event coordinates */}
              <div className="mt-3">
                <EventMap
                  latitude={event.latitude}
                  longitude={event.longitude}
                  title={event.title}
                />
              </div>

              <hr className="my-4" />

              {/* Price and going-count side by side */}
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wide text-gray-400">
                    Price
                  </p>
                  <p className="font-bold text-gray-900">
                    {formatPrice(event.price)}
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-xs font-semibold uppercase tracking-wide text-gray-400">
                    Going
                  </p>
                  <p className="font-bold text-gray-900">{event.goingCount}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
