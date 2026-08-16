import type { Event } from "@/types/event";
import EmptyState from "./EmptyState";
import EventCard from "./EventCard";

interface EventListProps {
  events: Event[];
}

export default function EventList({ events }: EventListProps) {
  if (events.length === 0) {
    return <EmptyState />;
  }

  return (
    <div>
      {events.map((event) => (
        <EventCard key={event.id} event={event} />
      ))}
    </div>
  );
}
