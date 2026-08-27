"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import EventList from "@/components/events/EventList";
import Pagination from "@/components/Pagination";
import { getGoingEvents, getSavedEvents } from "@/lib/api";
import type { EventPage } from "@/types/event";

const TAB_BASE = "border-b-2 pb-3 font-medium text-[15px] transition-colors";
const TAB_ACTIVE = "border-neutral-900 text-neutral-900";
const TAB_IDLE = "border-transparent text-neutral-500 hover:text-neutral-800";

export default function ProfileTabs() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // Tab and page live in the URL, so a refresh keeps them
  const tab = searchParams.get("tab") === "going" ? "going" : "saved";
  const page = Number(searchParams.get("page")) || 0;

  const [data, setData] = useState<EventPage | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setIsLoading(true);
    setError(null);

    const loadEvents = tab === "going" ? getGoingEvents : getSavedEvents;

    loadEvents(page, 9)
      .then(setData)
      .catch(() => setError("Could not load events"))
      .finally(() => setIsLoading(false));
  }, [tab, page]);

  // Switching a tab always starts from the first page
  function changeTab(nextTab: "saved" | "going") {
    router.push(`/profile?tab=${nextTab}`, { scroll: false });
  }

  return (
    <div>
      <div className="mb-6 flex gap-8 border-neutral-200 border-b">
        <button
          type="button"
          onClick={() => changeTab("saved")}
          className={`${TAB_BASE} ${tab === "saved" ? TAB_ACTIVE : TAB_IDLE}`}
        >
          Saved{" "}
          {tab === "saved" && data && (
            <span className="text-neutral-400">{data.totalElements}</span>
          )}
        </button>
        <button
          type="button"
          onClick={() => changeTab("going")}
          className={`${TAB_BASE} ${tab === "going" ? TAB_ACTIVE : TAB_IDLE}`}
        >
          Going{" "}
          {tab === "going" && data && (
            <span className="text-neutral-400">{data.totalElements}</span>
          )}
        </button>
      </div>

      {isLoading && (
        <p className="py-8 text-center text-neutral-500">Loading...</p>
      )}
      {error && <p className="py-8 text-center text-red-600">{error}</p>}
      {!isLoading && !error && data && (
        <>
          <EventList events={data.events} />
          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            hasNext={data.hasNext}
            basePath="/profile"
          />
        </>
      )}
    </div>
  );
}
