import type { Metadata } from "next";

import "./globals.css";
import { Navbar } from "@/components/layout/Navbar";

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
      <body className="flex h-screen flex-col overflow-hidden antialiased">
        <Navbar />
        <main className="min-h-0 flex-1">{children}</main>
      </body>
    </html>
  );
}
