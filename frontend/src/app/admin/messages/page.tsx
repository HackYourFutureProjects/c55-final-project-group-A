"use client";

import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import FeedbackCard from "@/components/admin/FeedbackCard";
import EmptyState from "@/components/EmptyState";
import Pagination from "@/components/Pagination";
import { getAdminFeedback } from "@/lib/api";
import type { Feedback, FeedbackPage } from "@/types/feedback";

function MessagesContent() {
  const searchParams = useSearchParams();
  const page = Number(searchParams.get("page") ?? 0);

  const [data, setData] = useState<FeedbackPage | null>(null);

  useEffect(() => {
    setData(null);
    getAdminFeedback(page).then(setData);
  }, [page]);

  function handleUpdated(updated: Feedback) {
    setData((current) =>
      current === null
        ? null
        : {
            ...current,
            feedbacks: current.feedbacks.map((item) =>
              item.id === updated.id ? updated : item,
            ),
          },
    );
  }

  return (
    <div className="mx-auto max-w-3xl px-6 py-8">
      <h1 className="font-bold text-2xl">Feedback</h1>
      <p className="mt-1 text-neutral-400 text-sm">
        {data?.totalElements} total
      </p>

      <div className="mt-6 space-y-4">
        {data?.feedbacks.map((feedback) => (
          <FeedbackCard
            key={feedback.id}
            feedback={feedback}
            onUpdated={handleUpdated}
          />
        ))}
      </div>

      {data?.feedbacks.length === 0 && (
        <EmptyState
          title="No feedback yet"
          hint="Submitted feedback will appear here"
        />
      )}

      {data && (
        <Pagination
          page={data.page}
          totalPages={data.totalPages}
          hasNext={data.hasNext}
          basePath="/admin/messages"
        />
      )}
    </div>
  );
}

export default function AdminMessagesPage() {
  return (
    <Suspense>
      <MessagesContent />
    </Suspense>
  );
}
