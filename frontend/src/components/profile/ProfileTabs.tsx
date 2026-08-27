"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import EventList from "@/components/events/EventList";
import Pagination from "@/components/Pagination";
import { getGoingEvents, getSavedEvents } from "@/lib/api";
import type { EventPage } from "@/types/event";

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
      <div>
        <button type="button" onClick={() => changeTab("saved")}>
          Saved
        </button>
        <button type="button" onClick={() => changeTab("going")}>
          Going
        </button>
      </div>

      {isLoading && <p>Loading...</p>}
      {error && <p>{error}</p>}

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
