import Link from "next/link";
import { formatPrice } from "@/lib/formatPrice";
import type { Event } from "@/types/event";

interface EventCardProps {
  event: Event;
}

function formatEventDate(startAt: string) {
  const date = new Date(startAt);
  const day = date.toLocaleDateString("en-GB", {
    timeZone: "Europe/Amsterdam",
    weekday: "short",
    day: "numeric",
    month: "short",
  });
  const time = date.toLocaleTimeString("en-GB", {
    timeZone: "Europe/Amsterdam",
    hour: "2-digit",
    minute: "2-digit",
  });
  return `${day} · ${time}`;
}

export default function EventCard({ event }: EventCardProps) {
  return (
    <Link href={`/events/${event.id}`} className="block h-full">
      <div className="flex h-full flex-col overflow-hidden rounded-2xl border border-gray-300 bg-gray-50 shadow-md transition-shadow hover:shadow-lg">
        {/* Photo area — taller now, ~1cm more vertical space */}
        <div className="relative flex h-52 shrink-0 items-center justify-center bg-orange-50">
          {event.imageUrl ? (
            <img
              src={event.imageUrl}
              alt={event.title}
              className="h-full w-full object-cover"
            />
          ) : (
            <p className="px-4 text-center font-medium text-orange-300">
              {event.title}
            </p>
          )}
        </div>

        <div className="flex flex-1 flex-col p-4">
          <div className="flex flex-wrap gap-2">
            {event.categories.map((category) => (
              <span
                key={category.id}
                className="text-xs font-semibold tracking-wide text-orange-500 uppercase"
              >
                {category.name}
              </span>
            ))}
          </div>
          {/* Clamped so a long title can't make one card taller than its row */}
          <h3 className="mt-1 line-clamp-2 text-lg font-bold text-gray-900">
            {event.title}
          </h3>
          <p className="mt-1 text-sm text-gray-600">
            {formatEventDate(event.startAt)}
          </p>
          <p className="text-sm text-gray-400">{event.cityName}</p>

          <hr className="my-3 mt-auto border-gray-200" />

          <div className="flex items-center justify-between">
            <span className="font-bold text-gray-900">
              {formatPrice(event.price)}
            </span>
            {event.goingCount !== undefined && (
              <span className="rounded-full bg-orange-50 px-3 py-1 text-sm font-medium text-orange-600">
                {event.goingCount} going
              </span>
            )}
          </div>
        </div>
      </div>
    </Link>
  );
}
