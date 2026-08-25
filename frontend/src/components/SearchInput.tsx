"use client";

import { useRouter, useSearchParams } from "next/navigation";
import type { SubmitEvent } from "react";

export function SearchInput() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // Writes the search term into the URL — the page re-renders
  // from the new address and fetches the filtered events
  function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    const formData = new FormData(event.currentTarget);
    const search = (formData.get("search") as string).trim();

    // Keep any filters already in the URL, replace only the search
    const params = new URLSearchParams(searchParams.toString());

    if (search) {
      params.set("search", search);
    } else {
      params.delete("search");
    }

    // A new search always starts from the first page
    params.delete("page");

    router.push(`/?${params.toString()}`, { scroll: false });
  }

  return (
    <form onSubmit={handleSubmit} className="w-full lg:w-80">
      <input
        type="text"
        name="search"
        placeholder="Search events..."
        defaultValue={searchParams.get("search") ?? ""}
        className="w-full rounded-full border border-neutral-200 bg-white px-5 py-3 outline-none focus:border-orange-500"
      />
    </form>
  );
}
