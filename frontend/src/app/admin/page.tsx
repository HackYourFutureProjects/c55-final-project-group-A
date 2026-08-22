import { ProtectedRoute } from "@/components/ProtectedRoute";

export default function AdminPage() {
  return (
    <ProtectedRoute adminOnly>
      <h1>Admin</h1>
    </ProtectedRoute>
  );
}
