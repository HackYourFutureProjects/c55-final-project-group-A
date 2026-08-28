"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import LocationFilter from "@/components/location/LocationFilter";
import type {
  Category,
  EventSort,
  PriceFilter,
  TimeOfDay,
} from "@/types/event";
import DateRangeFilter from "./DateRangeFilter";

// Category chips cycle through this palette by index
const CATEGORY_COLORS = [
  "bg-purple-50 text-purple-700 hover:bg-purple-100",
  "bg-orange-50 text-orange-700 hover:bg-orange-100",
  "bg-sky-50 text-sky-700 hover:bg-sky-100",
  "bg-amber-50 text-amber-700 hover:bg-amber-100",
  "bg-pink-50 text-pink-700 hover:bg-pink-100",
  "bg-emerald-50 text-emerald-700 hover:bg-emerald-100",
];

const PRICE_OPTIONS: { value: PriceFilter; label: string }[] = [
  { value: "FREE", label: "Free" },
  { value: "PAID", label: "Paid" },
  { value: "UNKNOWN", label: "Unknown" },
];

const SORT_OPTIONS: { value: EventSort; label: string }[] = [
  { value: "START_TIME_ASC", label: "Soonest" },
  { value: "POPULARITY_DESC", label: "Popular" },
  { value: "PRICE_ASC", label: "Cheapest" },
  { value: "PRICE_DESC", label: "Most expensive" },
];

const TIME_OPTIONS: { value: TimeOfDay; label: string }[] = [
  { value: "MORNING", label: "Morning" },
  { value: "AFTERNOON", label: "Afternoon" },
  { value: "EVENING", label: "Evening" },
];

// How many category chips to show before "Show all"
const VISIBLE_CATEGORY_COUNT = 5;

const CHIP_BASE =
  "rounded-full px-3 py-1.5 font-medium text-[13px] transition-colors";
const CHIP_ACTIVE =
  "bg-neutral-900 text-white ring-2 ring-neutral-900 ring-offset-1";
const CHIP_IDLE = "bg-neutral-100 text-neutral-600 hover:bg-neutral-200";

const SECTION_TITLE =
  "mb-3 flex items-center gap-2 font-semibold text-[11px] text-neutral-400 uppercase tracking-widest";

interface FilterSidebarProps {
  categories: Category[];
}

export default function FilterSidebar({ categories }: FilterSidebarProps) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [showAllCategories, setShowAllCategories] = useState(false);

  // Currently selected values, read straight from the URL
  const selectedCategories = searchParams.getAll("categoryIds");
  const selectedTimes = searchParams.getAll("timesOfDay");
  const selectedPrice = searchParams.get("price");
  const selectedSort = searchParams.get("sort");
  const dateFrom = searchParams.get("dateFrom");
  const dateTo = searchParams.get("dateTo");
  const latitude = searchParams.get("latitude");
  const longitude = searchParams.get("longitude");
  const radiusKm = searchParams.get("radiusKm");

  const hasFilters =
    selectedCategories.length > 0 ||
    selectedTimes.length > 0 ||
    dateFrom !== null ||
    selectedPrice !== null ||
    latitude !== null;

  // Long category lists stay collapsed to keep the sidebar compact
  const visibleCategories = showAllCategories
    ? categories
    : categories.slice(0, VISIBLE_CATEGORY_COUNT);

  // Writes the params to the URL; the server page then refetches events
  function apply(params: URLSearchParams) {
    // Any filter change invalidates the current page number
    params.delete("page");
    router.push(`/?${params.toString()}`, { scroll: false });
  }

  // Adds the value if missing, removes it if already selected
  function toggleRepeated(key: string, value: string) {
    const params = new URLSearchParams(searchParams.toString());
    const current = params.getAll(key);

    params.delete(key);
    for (const item of current) {
      if (item !== value) {
        params.append(key, item);
      }
    }
    if (!current.includes(value)) {
      params.append(key, value);
    }

    apply(params);
  }

  // Single-value filter: clicking the active option clears it
  function togglePrice(value: PriceFilter) {
    const params = new URLSearchParams(searchParams.toString());

    if (params.get("price") === value) {
      params.delete("price");
    } else {
      params.set("price", value);
    }

    apply(params);
  }

  // Clicking the active option goes back to the default order
  function setSort(value: EventSort) {
    const params = new URLSearchParams(searchParams.toString());

    if (params.get("sort") === value) {
      params.delete("sort");
    } else {
      params.set("sort", value);
    }

    apply(params);
  }

  // Both dates go in together — the backend rejects a half-filled range
  function setDateRange(from: string | null, to: string | null) {
    const params = new URLSearchParams(searchParams.toString());

    if (from && to) {
      params.set("dateFrom", from);
      params.set("dateTo", to);
    } else {
      params.delete("dateFrom");
      params.delete("dateTo");
    }

    apply(params);
  }

  // All three go in together — the radius filter only works when complete
  function setLocation(
    lat: number | null,
    lng: number | null,
    radius: number | null,
  ) {
    const params = new URLSearchParams(searchParams.toString());

    if (lat !== null && lng !== null && radius !== null) {
      params.set("latitude", String(lat));
      params.set("longitude", String(lng));
      params.set("radiusKm", String(radius));
    } else {
      params.delete("latitude");
      params.delete("longitude");
      params.delete("radiusKm");
    }

    apply(params);
  }

  // Drops every filter but keeps the user's search query
  function clearAll() {
    const params = new URLSearchParams();
    const search = searchParams.get("search");

    if (search) {
      params.set("search", search);
    }

    router.push(`/?${params.toString()}`);
  }

  return (
    <aside className="w-72 shrink-0 self-start space-y-6 rounded-2xl border border-orange-100 bg-white p-6 shadow-sm">
      <div className="flex items-center justify-between">
        <h2 className="font-bold text-lg">Filters</h2>
        {hasFilters && (
          <button
            type="button"
            onClick={clearAll}
            className="font-medium text-orange-600 text-sm hover:underline"
          >
            Clear all
          </button>
        )}
      </div>

      <section>
        <h3 className={SECTION_TITLE}>
          <span className="h-1.5 w-1.5 rounded-full bg-purple-400" />
          Category
        </h3>
        <div className="flex flex-wrap gap-2">
          {visibleCategories.map((category, index) => {
            const isActive = selectedCategories.includes(category.id);
            const idleColor = CATEGORY_COLORS[index % CATEGORY_COLORS.length];
            return (
              <button
                key={category.id}
                type="button"
                onClick={() => toggleRepeated("categoryIds", category.id)}
                aria-pressed={isActive}
                className={`${CHIP_BASE} ${isActive ? CHIP_ACTIVE : idleColor}`}
              >
                {category.name}
              </button>
            );
          })}
        </div>
        {categories.length > VISIBLE_CATEGORY_COUNT && (
          <button
            type="button"
            onClick={() => setShowAllCategories(!showAllCategories)}
            className="mt-3 font-medium text-neutral-500 text-xs hover:text-orange-600"
          >
            {showAllCategories
              ? "− Show less"
              : `+ Show all ${categories.length}`}
          </button>
        )}
      </section>
      <section>
        <h3 className={SECTION_TITLE}>
          <span className="h-1.5 w-1.5 rounded-full bg-amber-400" />
          Date
        </h3>
        <DateRangeFilter
          dateFrom={dateFrom}
          dateTo={dateTo}
          onChange={setDateRange}
        />
      </section>
      <section>
        <h3 className={SECTION_TITLE}>
          <span className="h-1.5 w-1.5 rounded-full bg-rose-400" />
          Location
        </h3>
        <LocationFilter
          latitude={latitude ? Number(latitude) : null}
          longitude={longitude ? Number(longitude) : null}
          radiusKm={radiusKm ? Number(radiusKm) : null}
          onChange={setLocation}
        />
      </section>

      <section>
        <h3 className={SECTION_TITLE}>
          <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" />
          Price
        </h3>
        <div className="flex flex-wrap gap-2">
          {PRICE_OPTIONS.map((option) => {
            const isActive = selectedPrice === option.value;
            return (
              <button
                key={option.value}
                type="button"
                onClick={() => togglePrice(option.value)}
                aria-pressed={isActive}
                className={`${CHIP_BASE} ${isActive ? CHIP_ACTIVE : CHIP_IDLE}`}
              >
                {option.label}
              </button>
            );
          })}
        </div>
      </section>

      <section>
        <h3 className={SECTION_TITLE}>
          <span className="h-1.5 w-1.5 rounded-full bg-sky-400" />
          Time of day
        </h3>
        <div className="flex flex-wrap gap-2">
          {TIME_OPTIONS.map((option) => {
            const isActive = selectedTimes.includes(option.value);
            return (
              <button
                key={option.value}
                type="button"
                onClick={() => toggleRepeated("timesOfDay", option.value)}
                aria-pressed={isActive}
                className={`${CHIP_BASE} ${isActive ? CHIP_ACTIVE : CHIP_IDLE}`}
              >
                {option.label}
              </button>
            );
          })}
        </div>
      </section>
      <section>
        <h3 className={SECTION_TITLE}>
          <span className="h-1.5 w-1.5 rounded-full bg-neutral-400" />
          Sort by
        </h3>
        <div className="flex flex-wrap gap-2">
          {SORT_OPTIONS.map((option) => {
            const isActive = selectedSort === option.value;
            return (
              <button
                key={option.value}
                type="button"
                onClick={() => setSort(option.value)}
                aria-pressed={isActive}
                className={`${CHIP_BASE} ${isActive ? CHIP_ACTIVE : CHIP_IDLE}`}
              >
                {option.label}
              </button>
            );
          })}
        </div>
      </section>
    </aside>
  );
}
