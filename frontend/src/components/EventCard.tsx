import type { Event } from "@/types/event";

interface EventCardProps {
  event: Event;
}

export default function EventCard({ event }: EventCardProps) {
  return (
    <div>
      {event.cancelled && <p>Cancelled</p>}
      {event.imageUrl ? (
        <img src={event.imageUrl} alt={event.title} />
      ) : (
        <p>{event.title}</p>
      )}

      <p>{event.categoryName}</p>
      <h3>{event.title}</h3>
      <p>{event.startAt}</p>
      <p>{event.cityName}</p>

      <hr />
      <div>
        <span>{event.price === 0 ? "Free" : `€${event.price}`} `</span>
        <span>{event.goingCount}</span>
      </div>
    </div>
  );
}
