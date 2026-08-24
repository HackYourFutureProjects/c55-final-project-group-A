import EventList from "@/components/EventList";
import FilterSidebar from "@/components/filters/FilterSidebar";
import HomeBanner from "@/components/HomeBanner";
import Pagination from "@/components/Pagination";
import { getCategories, getEvents } from "@/lib/api";
import type { EventFilters, PriceFilter, TimeOfDay } from "@/types/event";

// Skip build-time prerendering: this page fetches live events, and the
// Docker build has no backend to reach. Also means fresh data on every visit
export const dynamic = "force-dynamic";

// searchParams is everything after "?" in the URL.
// Values can be a single string (?radiusKm=10), an array when the same
// key repeats (?categoryIds=a&categoryIds=b), or undefined when absent.
// It arrives as a Promise in Next 15+, like params on the detail page
interface HomeProps {
  searchParams: Promise<{ [key: string]: string | string[] | undefined }>;
}

// A single value arrives as a string, several as an array — normalise both
function toArray(value: string | string[] | undefined): string[] | undefined {
  if (!value) return undefined;
  return Array.isArray(value) ? value : [value];
}

// URL values are always strings (or arrays when a key repeats),
// but getEvents expects numbers — so we convert here
function toFilters(params: {
  [key: string]: string | string[] | undefined;
}): EventFilters {
  return {
    page: params.page ? Number(params.page) : 0,
    search: typeof params.search === "string" ? params.search : undefined,
    categoryIds: toArray(params.categoryIds),
    latitude: params.latitude ? Number(params.latitude) : undefined,
    longitude: params.longitude ? Number(params.longitude) : undefined,
    radiusKm: params.radiusKm ? Number(params.radiusKm) : undefined,
    price: params.price as PriceFilter | undefined,
    timesOfDay: toArray(params.timesOfDay) as TimeOfDay[] | undefined,
    dateFrom: typeof params.dateFrom === "string" ? params.dateFrom : undefined,
    dateTo: typeof params.dateTo === "string" ? params.dateTo : undefined,
  };
}

export default async function Home({ searchParams }: HomeProps) {
  const params = await searchParams;

  // Both requests start at the same time instead of one after the other
  const [page, categories] = await Promise.all([
    getEvents(toFilters(params)),
    getCategories(),
  ]);

  return (
    <main className="mx-auto w-full max-w-10xl px-6 py-8">
      <HomeBanner eventCount={page.totalElements} />
      <div className="mt-10 flex items-start gap-8">
        <FilterSidebar categories={categories} />
        <div className="flex-1">
          <EventList events={page.events} />
          <Pagination
            page={page.page}
            totalPages={page.totalPages}
            hasNext={page.hasNext}
          />
        </div>
      </div>
    </main>
  );
}
