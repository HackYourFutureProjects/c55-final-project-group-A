"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/context/AuthContext";

export function Navbar() {
  const { user } = useAuth();
  const pathname = usePathname();

  return (
    <header className="flex h-16 items-center justify-between border-b border-neutral-200 bg-white px-8">
      <div className="flex items-center gap-8">
        <div className="flex items-center gap-2">
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-orange-600 text-white">
            📍
          </span>
          <span className="text-lg font-bold">Loc</span>
        </div>

        <nav className="flex items-center gap-6 text-lg font-medium text-neutral-600">
          |
          <Link
            href="/"
            className={
              pathname === "/"
                ? "font-semibold text-orange-600"
                : "hover:text-orange-600"
            }
          >
            Home
          </Link>
          |
          <Link
            href="/feedback"
            className={
              pathname === "/feedback"
                ? "font-semibold text-orange-600"
                : "hover:text-orange-600"
            }
          >
            Feedback
          </Link>
          |{/* Only admins see the link back into the admin area */}
          {user?.role === "admin" && (
            <Link
              href="/admin"
              className={
                pathname.startsWith("/admin")
                  ? "font-semibold text-orange-600"
                  : "hover:text-orange-600"
              }
            >
              Dashboard
            </Link>
          )}
        </nav>
      </div>

      <div className="flex items-center gap-4">
        {user ? (
          <Link
            href="/profile"
            className="flex items-center gap-2 hover:opacity-80"
          >
            <span className="flex h-9 w-9 items-center justify-center rounded-full bg-orange-100 text-sm font-semibold text-orange-700">
              {user.role === "admin" ? "A" : user.name.charAt(0).toUpperCase()}
            </span>
            <span className="text-sm font-semibold text-neutral-700">
              {user.name.split(" ")[0]}
            </span>
          </Link>
        ) : (
          <>
            <Link
              href="/login"
              className="text-sm font-semibold text-neutral-700 hover:text-orange-600"
            >
              Log in
            </Link>
            <Link
              href="/login?tab=register"
              className="rounded-full bg-orange-600 px-4 py-2 text-sm font-semibold text-white hover:bg-orange-700"
            >
              Sign up
            </Link>
          </>
        )}
      </div>
    </header>
  );
}
