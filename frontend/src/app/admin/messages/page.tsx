"use client";

import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import Pagination from "@/components/Pagination";
import FeedbackCard from "@/components/admin/FeedbackCard";
import { getAdminFeedback } from "@/lib/api";
import type { Feedback, FeedbackPage } from "@/types/feedback";
import EmptyState from "@/components/EmptyState";


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
    <div>
      <p>Total: {data?.totalElements}</p>

      {data?.feedbacks.map((feedback) => (
        <FeedbackCard
          key={feedback.id}
          feedback={feedback}
          onUpdated={handleUpdated}
        />
      ))}

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
