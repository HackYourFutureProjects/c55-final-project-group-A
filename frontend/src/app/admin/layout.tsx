// Shared navigation for the admin section.

"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();

  // The underline is black on the current page, invisible on the others
  const activeTab = "border-b-2 border-neutral-900 py-4 font-semibold text-sm";
  const idleTab =
    "border-b-2 border-transparent py-4 font-semibold text-sm text-neutral-500 hover:text-neutral-900";

  return (
    <div>
      <div className="border-neutral-200 border-b">
        <nav className="mx-auto flex max-w-7xl gap-6 px-6">
          <Link
            href="/admin"
            className={pathname === "/admin" ? activeTab : idleTab}
          >
            Create event
          </Link>

          <Link
            href="/admin/events"
            className={pathname === "/admin/events" ? activeTab : idleTab}
          >
            All events
          </Link>

          <Link
            href="/admin/messages"
            className={pathname === "/admin/messages" ? activeTab : idleTab}
          >
            Messages
          </Link>
        </nav>
      </div>
      {children}
    </div>
  );
}
