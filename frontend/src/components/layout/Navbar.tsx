import Link from "next/link";

export function Navbar() {
  return (
    <header className="flex h-16 items-center justify-between border-b border-neutral-200 bg-white px-8">
      <div className="flex items-center gap-8">
        <div className="flex items-center gap-2">
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-orange-600 text-white">
            📍
          </span>
          <span className="text-lg font-bold">Loc</span>
        </div>

        <nav className="flex items-center gap-6 text-sm font-medium text-neutral-600">
          <Link href="/" className="hover:text-orange-600">
            Home
          </Link>
          <Link href="/feedback" className="hover:text-orange-600">
            Feedback
          </Link>
        </nav>
      </div>

      <div className="flex items-center gap-4">
        {/* TODO: swap for "My Profile" (or admin equivalent) once backend auth lands */}
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
      </div>
    </header>
  );
}
