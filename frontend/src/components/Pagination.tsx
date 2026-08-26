"use client";

import { useRouter, useSearchParams } from "next/navigation";

interface PaginationProps {
  page: number;
  totalPages: number;
  hasNext: boolean;
  basePath?: string; // where to push to; defaults to the homepage
}

export default function Pagination({
  page,
  totalPages,
  hasNext,
  basePath = "/",
}: PaginationProps) {
  const router = useRouter();
  const searchParams = useSearchParams();

  function goToPage(nextPage: number) {
    const params = new URLSearchParams(searchParams.toString());
    params.set("page", String(nextPage));
    router.push(`${basePath}?${params.toString()}`, { scroll: false });
  }
  if (totalPages === 0) {
    return null;
  }

  return (
    <div className="mt-8 flex items-center justify-center gap-4">
      <button
        type="button"
        onClick={() => goToPage(page - 1)}
        disabled={page === 0}
        className="rounded-full border border-neutral-200 px-5 py-2 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-40"
      >
        Back
      </button>

      <span className="text-sm text-neutral-500">
        Page {page + 1} of {totalPages}
      </span>

      <button
        type="button"
        onClick={() => goToPage(page + 1)}
        disabled={!hasNext}
        className="rounded-full border border-neutral-200 px-5 py-2 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-40"
      >
        Next
      </button>
    </div>
  );
}
