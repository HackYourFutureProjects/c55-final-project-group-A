"use client";

import { type SubmitEvent, useState } from "react";
import { submitFeedback } from "@/lib/api";
import type { FeedbackTopic } from "@/types/feedback";

// emoji scale — index + 1 is the rating value sent to the backend
const RATINGS = ["😔", "😐", "🙂", "😄", "🤩"];

const CHIP_BASE = "rounded-full px-5 py-2 font-semibold text-sm";
const CHIP_ACTIVE = "bg-neutral-900 text-white";
const CHIP_IDLE = "text-neutral-600 hover:bg-neutral-50";

const INPUT =
  "w-full rounded-lg border border-neutral-200 px-3 py-2 outline-none focus:border-neutral-900";

export default function FeedbackPage() {
  const [topic, setTopic] = useState<FeedbackTopic>("app");
  const [rating, setRating] = useState<number | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    // rating is a primitive int with @Min(1) on the backend, so it is required
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
        // empty string becomes undefined so the key is left out of the JSON
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
    return (
      <div className="mx-auto max-w-2xl px-6 py-24">
        <div className="rounded-2xl border border-neutral-200 bg-orange-50 p-12 text-center">
          <p className="text-6xl">🎉</p>
          <h1 className="mt-6 font-bold text-3xl">Thank you!</h1>
          <p className="mt-3 text-neutral-600">
            Your feedback has been sent to the team.
          </p>
          <button
            type="button"
            onClick={() => {
              setSubmitted(false);
              setRating(null);
              setIsSubmitting(false);
            }}
            className="mt-8 rounded-full bg-orange-600 px-6 py-3 font-semibold text-sm text-white hover:bg-orange-700"
          >
            Send another
          </button>
        </div>
      </div>
    );
  }
  return (
    <div className="mx-auto max-w-3xl px-6 py-10">
      <div className="rounded-3xl bg-gradient-to-br from-orange-500 to-orange-600 p-10 shadow-lg shadow-orange-200">
        <p className="inline-block rounded-full bg-white/20 px-3 py-1 font-semibold text-white text-xs tracking-widest">
          FEEDBACK
        </p>
        <h1 className="mt-5 font-bold text-4xl text-white">
          How is Loc working for you?
        </h1>
        <p className="mt-4 max-w-xl text-lg text-orange-50">
          Anonymous by default — tell us about the app itself, or about an event
          you went to.
        </p>
      </div>

      <form
        key={topic}
        onSubmit={handleSubmit}
        className="-mt-6 relative rounded-3xl border border-neutral-100 bg-white p-10 shadow-xl shadow-neutral-200/60"
      >
        <p className="font-semibold text-base">What is your feedback about?</p>
        <div className="mt-4 inline-flex rounded-full bg-neutral-100 p-1">
          <button
            type="button"
            onClick={() => setTopic("app")}
            className={`${CHIP_BASE} ${topic === "app" ? CHIP_ACTIVE : CHIP_IDLE}`}
          >
            The app
          </button>
          <button
            type="button"
            onClick={() => setTopic("event")}
            className={`${CHIP_BASE} ${topic === "event" ? CHIP_ACTIVE : CHIP_IDLE}`}
          >
            An event
          </button>
        </div>

        {topic === "event" && (
          <div className="mt-8 border-neutral-100 border-t pt-8">
            <label
              htmlFor="eventTitle"
              className="block font-semibold text-base"
            >
              Which event?
            </label>
            <input
              id="eventTitle"
              name="eventTitle"
              maxLength={255}
              placeholder="Event title"
              className={`mt-3 ${INPUT}`}
            />
          </div>
        )}

        <div className="mt-8 border-neutral-100 border-t pt-8">
          <p className="font-semibold text-base">How would you rate it?</p>
          <div className="mt-4 grid grid-cols-5 gap-3">
            {RATINGS.map((emoji, index) => (
              <button
                key={emoji}
                type="button"
                onClick={() => setRating(index + 1)}
                className={`rounded-2xl border-2 py-5 text-4xl transition focus:outline-none ${
                  rating === index + 1
                    ? "scale-105 border-orange-500 bg-orange-50 shadow-md shadow-orange-100"
                    : "border-neutral-100 hover:border-orange-200 hover:bg-orange-50"
                }`}
              >
                {emoji}
              </button>
            ))}
          </div>
        </div>

        <div className="mt-8 border-neutral-100 border-t pt-8">
          <label htmlFor="message" className="block font-semibold text-base">
            Tell us more
          </label>
          <textarea
            id="message"
            name="message"
            maxLength={3000}
            rows={5}
            placeholder="What worked, what didn't..."
            className={`mt-3 resize-none ${INPUT}`}
          />
        </div>

        <div className="mt-8 border-neutral-100 border-t pt-8">
          <p className="font-semibold text-base">
            Name &amp; email{" "}
            <span className="font-normal text-neutral-400">(optional)</span>
          </p>
          <div className="mt-3 grid gap-4 sm:grid-cols-2">
            <input
              id="senderName"
              name="senderName"
              maxLength={150}
              placeholder="Your name"
              className={INPUT}
            />
            <input
              id="senderEmail"
              name="senderEmail"
              type="email"
              placeholder="Your email"
              className={INPUT}
            />
          </div>
        </div>

        {error && <p className="mt-5 text-red-600 text-sm">{error}</p>}

        <div className="mt-8 flex items-center justify-between border-neutral-100 border-t pt-8">
          <p className="text-neutral-400 text-sm">
            Stored for the team's review
          </p>
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-full bg-orange-600 px-8 py-3 font-semibold text-base text-white shadow-lg shadow-orange-200 transition hover:bg-orange-700 disabled:opacity-50"
          >
            {isSubmitting ? "Sending..." : "Send feedback"}
          </button>
        </div>
      </form>
    </div>
  );
}
