// Inbox: all notifications for the current user, newest first.

"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { getNotifications, openNotification } from "@/lib/api";
import type { NotificationPage } from "@/types/notification";

export default function NotificationsPage() {
  const router = useRouter();
  const [data, setData] = useState<NotificationPage | null>(null);

  useEffect(() => {
    getNotifications()
      .then(setData)
      .catch(() => setData(null));
  }, []);

  async function open(id: string, linkPath: string | null | undefined) {
    const updated = await openNotification(id);

    // Replace the one item that changed instead of refetching the page.
    setData((current) =>
      current
        ? {
            ...current,
            notifications: current.notifications.map((item) =>
              item.id === updated.id ? updated : item,
            ),
          }
        : current,
    );

    if (linkPath) {
      router.push(linkPath);
    }
  }

  if (!data) {
    return (
      <main className="mx-auto max-w-3xl px-6 py-8">
        <p className="text-neutral-400">Loading...</p>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-3xl px-6 py-8">
      <h1 className="mb-6 font-bold text-2xl">Notifications</h1>

      {data.notifications.length === 0 ? (
        <p className="text-neutral-500">Nothing here yet.</p>
      ) : (
        <div className="space-y-3">
          {data.notifications.map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => open(item.id, item.linkPath)}
              className={
                item.read
                  ? "block w-full rounded-2xl border border-neutral-200 bg-white p-6 text-left hover:bg-neutral-50"
                  : "block w-full rounded-2xl border border-neutral-200 bg-amber-50 p-6 text-left hover:bg-amber-100"
              }
            >
              <p className="font-semibold text-neutral-900">{item.title}</p>
              <p className="mt-1 text-neutral-600 text-sm">{item.body}</p>
            </button>
          ))}
        </div>
      )}
    </main>
  );
}
