import Link from "next/link";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div>
      <nav>
        <Link href="/admin">Create event</Link>
        <Link href="/admin/events">All events</Link>
        <Link href="/admin/messages">Messages</Link>
      </nav>
      <main>{children}</main>
    </div>
  );
}
