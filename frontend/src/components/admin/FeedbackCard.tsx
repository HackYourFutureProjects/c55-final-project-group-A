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
    <div>
      <p>
        {RATING_EMOJI[feedback.rating - 1]} {feedback.rating}/5
      </p>
      <p>
        {feedback.topic === "app" ? "The app" : "An event"} ·{" "}
        {feedback.isReviewed ? "Reviewed" : "New"}
      </p>
      <p>{new Date(feedback.createdAt).toLocaleString()}</p>

      {feedback.eventTitle && <p>{feedback.eventTitle}</p>}
      {feedback.message && <p>{feedback.message}</p>}
      {feedback.senderName && <p>{feedback.senderName}</p>}
      {feedback.senderEmail && <p>{feedback.senderEmail}</p>}

      <button type="button" onClick={handleToggle} disabled={isBusy}>
        {feedback.isReviewed ? "Mark as new" : "Mark reviewed"}
      </button>
    </div>
  );
}