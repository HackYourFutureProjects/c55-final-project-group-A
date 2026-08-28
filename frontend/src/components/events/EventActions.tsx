// Save + Going buttons for the event detail page.
// Client component: needs useState for optimistic UI updates and onClick handlers.
// Guests are redirected to /login instead of triggering a request.

"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { markGoing, saveEvent, unmarkGoing, unsaveEvent } from "@/lib/api";

interface EventActionsProps {
  eventId: string;
  initialIsSaved: boolean;
  initialIsGoing: boolean;
  initialGoingCount: number;
}

export default function EventActions({
  eventId,
  initialIsSaved,
  initialIsGoing,
  initialGoingCount,
}: EventActionsProps) {
  const { user } = useAuth();
  const router = useRouter();

  const [isSaved, setIsSaved] = useState(initialIsSaved);
  const [isGoing, setIsGoing] = useState(initialIsGoing);
  const [goingCount, setGoingCount] = useState(initialGoingCount);

  async function handleSaveClick() {
    if (!user) {
      router.push("/login");
      return;
    }

    const nextIsSaved = !isSaved;
    setIsSaved(nextIsSaved);

    try {
      if (nextIsSaved) {
        await saveEvent(eventId);
      } else {
        await unsaveEvent(eventId);
      }
    } catch {
      setIsSaved(!nextIsSaved);
    }
  }

  async function handleGoingClick(nextIsGoing: boolean) {
    if (!user) {
      router.push("/login");
      return;
    }

    if (nextIsGoing === isGoing) return;

    setIsGoing(nextIsGoing);
    setGoingCount((count) => (nextIsGoing ? count + 1 : count - 1));

    try {
      if (nextIsGoing) {
        await markGoing(eventId);
      } else {
        await unmarkGoing(eventId);
      }
    } catch {
      setIsGoing(!nextIsGoing);
      setGoingCount((count) => (nextIsGoing ? count - 1 : count + 1));
    }
  }

  return (
    <div className="flex flex-wrap items-center gap-3">
      <button
        type="button"
        onClick={handleSaveClick}
        className={`flex items-center gap-2 rounded-full px-5 py-2.5 font-semibold transition-colors ${
          isSaved
            ? "bg-neutral-900 text-white hover:bg-neutral-800"
            : "bg-orange-600 text-white hover:bg-orange-700"
        }`}
      >
        <span>{isSaved ? "❤️" : "🤍"}</span>
        {isSaved ? "Saved" : "Save event"}
      </button>
      <button
        type="button"
        onClick={() => handleGoingClick(!isGoing)}
        className={`rounded-full px-5 py-2.5 font-semibold transition-colors ${
          isGoing
            ? "bg-neutral-900 text-white hover:bg-neutral-800"
            : "bg-orange-600 text-white hover:bg-orange-700"
        }`}
      >
        {isGoing ? "Going" : "Join"}
      </button>
    </div>
  );
}
