import EventList from "@/components/EventList";
import HomeBanner from "@/components/HomeBanner";
import { getEvents } from "@/lib/api";

export const dynamic = "force-dynamic";

export default async function Home() {
  const page = await getEvents();

  return (
    <main className="mx-auto w-full max-w-10xl px-6 py-8">
      <HomeBanner eventCount={page.totalElements} />
      <EventList events={page.events} />
    </main>
  );
}
