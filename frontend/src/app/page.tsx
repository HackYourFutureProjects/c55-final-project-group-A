import EventList from "@/components/EventList";
import HomeBanner from "@/components/HomeBanner";
import type { Event } from "@/types/event";

// TODO: remove sample data once GET /api/events returns real events
const sampleEvents: Event[] = [
  {
    id: "1",
    title: "Jazz Night in Amsterdam",
    description: "An evening of live jazz music",
    categoryName: "Music",
    startAt: "2026-09-12T19:00:00",
    endAt: "2026-09-12T22:00:00",
    price: 15,
    street: "Prinsengracht",
    houseNumber: "10",
    postalCode: "1015DX",
    cityName: "Amsterdam",
    province: "Noord-Holland",
    imageUrl: null,
    goingCount: 42,
    cancelled: false,
  },
  {
    id: "2",
    title: "Free Yoga in the Park",
    description: null,
    categoryName: "Sports",
    startAt: "2026-09-14T09:00:00",
    endAt: "2026-09-14T10:00:00",
    price: 0,
    street: "Vondelpark",
    houseNumber: "1",
    postalCode: "1071AA",
    cityName: "Amsterdam",
    province: "Noord-Holland",
    imageUrl: null,
    goingCount: 8,
    cancelled: false,
  },
];

export default function Home() {
  return (
    <main>
      <HomeBanner eventCount={sampleEvents.length} />
      <EventList events={sampleEvents} />
    </main>
  );
}
