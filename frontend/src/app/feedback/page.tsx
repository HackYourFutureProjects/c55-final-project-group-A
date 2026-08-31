"use client";

import { type SubmitEvent, useState } from "react";
import { submitFeedback } from "@/lib/api";
import type { FeedbackTopic } from "@/types/feedback";

// emoji scale — index + 1 is the rating value sent to the backend
const RATINGS = ["😔", "😐", "🙂", "😄", "🤩"];

export default function FeedbackPage() {
  const [topic, setTopic] = useState<FeedbackTopic>("app");
  const [rating, setRating] = useState<number | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    if (rating === null) {
      setError("Please choose a rating");
      return;
    }

    const form = new FormData(event.currentTarget);

    setIsSubmitting(true);
    setError(null);

    try {
      await submitFeedback({
        topic,
        rating,
        eventTitle: form.get("eventTitle")?.toString() || undefined,
        message: form.get("message")?.toString() || undefined,
        senderName: form.get("senderName")?.toString() || undefined,
        senderEmail: form.get("senderEmail")?.toString() || undefined,
      });
      setSubmitted(true);
    } catch {
      setError("Could not send your feedback. Please try again.");
      setIsSubmitting(false);
    }
  }

  if (submitted) {
    return <p>Thank you! Your feedback has been sent.</p>;
  }

  return (
    <form onSubmit={handleSubmit}>
      <p>What is your feedback about?</p>
      <button type="button" onClick={() => setTopic("app")}>
        The app
      </button>
      <button type="button" onClick={() => setTopic("event")}>
        An event
      </button>

      {topic === "event" && (
        <div>
          <label htmlFor="eventTitle">Which event?</label>
          <input id="eventTitle" name="eventTitle" maxLength={255} />
        </div>
      )}

      <p>How would you rate it?</p>
      {RATINGS.map((emoji, index) => (
        <button key={emoji} type="button" onClick={() => setRating(index + 1)}>
          {emoji}
        </button>
      ))}

      <label htmlFor="message">Tell us more</label>
      <textarea id="message" name="message" maxLength={3000} />

      <label htmlFor="senderName">Name</label>
      <input id="senderName" name="senderName" maxLength={150} />

      <label htmlFor="senderEmail">Email</label>
      <input id="senderEmail" name="senderEmail" type="email" />

      {error && <p>{error}</p>}

      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Sending..." : "Send feedback"}
      </button>
    </form>
  );
}
