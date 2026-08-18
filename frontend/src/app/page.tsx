import EventList from "@/components/EventList";
import HomeBanner from "@/components/HomeBanner";
import type { EventPage } from "@/types/event";

const samplePage: EventPage = {
  events: [
    {
      id: "40000000-0000-0000-0000-000000000001",
      title: "Jazz Night in Amsterdam",
      categoryName: "Music",
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
      categoryName: "Sports",
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

export default function Home() {
  return (
    <main className="mx-auto w-full max-w-10xl px-6 py-8">
      <HomeBanner eventCount={samplePage.totalElements} />
      <EventList events={samplePage.events} />
    </main>
  );
}
