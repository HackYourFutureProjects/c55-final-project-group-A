import { ProtectedRoute } from "@/components/ProtectedRoute";

export default function ProfilePage() {
  return (
    <ProtectedRoute>
      <h1>Profile</h1>
    </ProtectedRoute>
  );
}
