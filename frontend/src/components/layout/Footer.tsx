import Link from "next/link";

export function Footer() {
  return (
    <footer className="border-t border-neutral-200 py-6 text-center text-sm text-neutral-500">
      <div className="flex flex-wrap items-center justify-center gap-4">
        <p>Loc · {new Date().getFullYear()} · Built at HackYourFuture</p>

        <div className="flex gap-4">
          <Link
            href="/privacy-policy"
            className="hover:text-neutral-700 hover:underline"
          >
            Privacy Policy
          </Link>
          <Link
            href="/terms-of-service"
            className="hover:text-neutral-700 hover:underline"
          >
            Terms of Service
          </Link>
        </div>
      </div>
    </footer>
  );
}
