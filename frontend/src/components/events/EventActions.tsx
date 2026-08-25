// Save + Going buttons for the event detail page.
// Client component: needs useState for optimistic UI updates and onClick handlers.
// Guests are redirected to /login instead of triggering a request.


"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { saveEvent, unsaveEvent, markGoing, unmarkGoing } from "@/lib/api";

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

  async function handleGoingClick() {
    if (!user) {
      router.push("/login");
      return;
    }

    const nextIsGoing = !isGoing;
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
    <div>
      <button onClick={handleSaveClick}>
        {isSaved ? "Saved" : "Save"}
      </button>
      <button onClick={handleGoingClick}>
        {isGoing ? "Going" : "Going?"} ({goingCount})
      </button>
    </div>
  );
}