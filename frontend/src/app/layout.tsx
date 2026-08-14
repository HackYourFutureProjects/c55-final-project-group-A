import Link from "next/link";
import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Loc",
  description: "Find local events",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <header>
          <nav>
            <Link href="/">Events</Link>
            {" | "}
            <Link href="/feedback">Feedback</Link>
          </nav>
        </header>
        <main>{children}</main>
      </body>
    </html>
  );
}
