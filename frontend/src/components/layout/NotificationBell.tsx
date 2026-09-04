// Unread badge in the navbar. Polls the count so it updates without a reload.

"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getUnreadCount } from "@/lib/api";

const POLL_INTERVAL_MS = 5000;

export default function NotificationBell() {
  const [count, setCount] = useState(0);

  useEffect(() => {
    async function load() {
      try {
        setCount(await getUnreadCount());
      } catch {
        setCount(0);
      }
    }

    load();
    const timer = setInterval(load, POLL_INTERVAL_MS);
    return () => clearInterval(timer);
  }, []);

  return (
    <Link
      href="/notifications"
      aria-label="Notifications"
      className="relative flex h-9 w-9 items-center justify-center rounded-full hover:bg-neutral-100"
    >
      <span className="text-lg">🔔</span>
      {count > 0 && (
        <span className="-top-0.5 -right-0.5 absolute flex h-5 min-w-5 items-center justify-center rounded-full bg-orange-600 px-1 font-semibold text-white text-xs">
          {count}
        </span>
      )}
    </Link>
  );
}
