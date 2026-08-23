"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { ProtectedRoute } from "@/components/ProtectedRoute";
import { DeleteAccountModal } from "@/components/profile/DeleteAccountModal";
import { EditProfileModal } from "@/components/profile/EditProfileModal";
import { useAuth } from "@/context/AuthContext";
import { deleteCurrentUser, logout } from "@/lib/api";

// Page shell: ProtectedRoute redirects guests to /login
// before any profile content is rendered
export default function ProfilePage() {
  return (
    <ProtectedRoute>
      <ProfileContent />
    </ProtectedRoute>
  );
}

// Separate component so useAuth() runs only after ProtectedRoute
// has confirmed there is a logged-in user
function ProfileContent() {
  const { user, refresh } = useAuth();
  const router = useRouter();
  const [isEditing, setIsEditing] = useState(false);
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);
  // Clears the session on the backend, resets the auth state,
  // then sends the user to the homepage
  async function handleLogout() {
    await logout();
    await refresh();
    router.push("/");
  }

  // ProtectedRoute already guarantees a user exists,
  // but TypeScript still needs the narrowing
  if (!user) {
    return null;
  }
  // Permanently deletes the account, clears the auth state,
  // then sends the user to the homepage
  async function handleDeleteAccount() {
    await deleteCurrentUser();
    await refresh();
    router.push("/");
  }

  return (
    <main>
      <h1>{user.name}</h1>
      <p>{user.email}</p>
      <p>{user.location ?? "No location set"}</p>

      <button type="button" onClick={() => setIsEditing(true)}>
        Edit profile
      </button>

      <button type="button" onClick={handleLogout}>
        Log out
      </button>

      {isEditing && (
        <EditProfileModal user={user} onClose={() => setIsEditing(false)} />
      )}
      <button type="button" onClick={() => setIsDeleteOpen(true)}>
        Delete account
      </button>

      {isDeleteOpen && (
        <DeleteAccountModal
          onConfirm={handleDeleteAccount}
          onClose={() => setIsDeleteOpen(false)}
        />
      )}
    </main>
  );
}
