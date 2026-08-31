"use client";

import { useState } from "react";
import { updateFeedbackReviewed } from "@/lib/api";
import type { Feedback } from "@/types/feedback";

const RATING_EMOJI = ["😔", "😐", "🙂", "😄", "🤩"];

type Props = {
  feedback: Feedback;
  onUpdated: (updated: Feedback) => void;
};

export default function FeedbackCard({ feedback, onUpdated }: Props) {
  const [isBusy, setIsBusy] = useState(false);

  async function handleToggle() {
    setIsBusy(true);
    try {
      const updated = await updateFeedbackReviewed(
        feedback.id,
        !feedback.isReviewed,
      );
      onUpdated(updated);
    } catch {
      setIsBusy(false);
    }
  }

  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-6">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-center gap-3">
          <span className="text-3xl">{RATING_EMOJI[feedback.rating - 1]}</span>
          <div>
            <p className="font-semibold text-sm">
              {feedback.topic === "app" ? "The app" : "An event"}
            </p>
            <p className="text-neutral-400 text-sm">
              {new Date(feedback.createdAt).toLocaleString("en-GB", {
                day: "numeric",
                month: "short",
                year: "numeric",
                hour: "2-digit",
                minute: "2-digit",
              })}
            </p>
          </div>
        </div>

        <span
          className={`shrink-0 rounded-full px-3 py-1 font-semibold text-xs ${
            feedback.isReviewed
              ? "bg-neutral-100 text-neutral-500"
              : "bg-amber-50 text-amber-700"
          }`}
        >
          {feedback.isReviewed ? "Reviewed" : "New"}
        </span>
      </div>

      {feedback.eventTitle && (
        <p className="mt-4 font-semibold text-sm">{feedback.eventTitle}</p>
      )}

      {feedback.message && (
        <p className="mt-3 text-neutral-700">{feedback.message}</p>
      )}

      <div className="mt-5 flex items-center justify-between gap-4 border-t pt-4">
        <p className="min-w-0 text-neutral-400 text-sm">
          {feedback.senderName ?? "Anonymous"}
          {feedback.senderEmail && ` · ${feedback.senderEmail}`}
        </p>

        <button
          type="button"
          onClick={handleToggle}
          disabled={isBusy}
          className="shrink-0 rounded-full border border-neutral-200 px-4 py-2 font-semibold text-sm transition hover:bg-neutral-50 disabled:opacity-50"
        >
          {feedback.isReviewed ? "Mark as new" : "Mark reviewed"}
        </button>
      </div>
    </div>
  );
}
