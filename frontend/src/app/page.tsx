import EventList from "@/components/EventList";
import HomeBanner from "@/components/HomeBanner";
import { getEvents } from "@/lib/api";
import type { EventPage } from "@/types/event";

const samplePage: EventPage = {
  events: [
    {
      id: "40000000-0000-0000-0000-000000000001",
      title: "Jazz Night in Amsterdam",
      categories: [
        { id: "c3551998-6b23-446c-a503-d18de49861a1", name: "Music" },
      ],
      startAt: "2026-09-12T17:00:00Z",
      endAt: "2026-09-12T21:30:00Z",
      price: 15,
      street: "Prinsengracht",
      houseNumber: "10",
      postalCode: "1015 DX",
      cityName: "Amsterdam",
      province: "North Holland",
      imageUrl: null,
      goingCount: 42,
      cancelled: false,
    },
    {
      id: "40000000-0000-0000-0000-000000000002",
      title: "Free Yoga in the Park",
      categories: [
        {
          id: "ef8a608b-810a-4717-a623-b869d8f2bd9e",
          name: "Sports & Fitness",
        },
      ],
      startAt: "2026-09-14T07:00:00Z",
      endAt: "2026-09-14T08:00:00Z",
      price: 0,
      street: "Vondelpark",
      houseNumber: "1",
      postalCode: "1071 AA",
      cityName: "Amsterdam",
      province: "North Holland",
      imageUrl: null,
      goingCount: 8,
      cancelled: false,
    },
  ],
  page: 0,
  size: 9,
  totalElements: 2,
  totalPages: 1,
  hasNext: false,
};

export default async function Home() {
  const page = await getEvents();

  return (
    <main className="mx-auto w-full max-w-10xl px-6 py-8">
      <HomeBanner eventCount={page.totalElements} />
      <EventList events={page.events} />
    </main>
  );
}
