// Up to five events similar to the one being viewed.
import EventCard from "@/components/events/EventCard";
import { getSimilarEvents } from "@/lib/api";

interface SimilarEventsProps {
  eventId: string;
}

export default async function SimilarEvents({ eventId }: SimilarEventsProps) {
  const events = (await getSimilarEvents(eventId).catch(() => [])).slice(0, 3);

  if (events.length === 0) {
    return null;
  }

  return (
    <section className="mt-8">
      <h2 className="font-bold text-gray-900 text-xl">Similar events</h2>
      <div className="mt-4 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {events.map((event) => (
          <EventCard key={event.id} event={event} />
        ))}
      </div>
    </section>
  );
}
