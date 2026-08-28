"use client";

import { useRouter } from "next/navigation";
import { Suspense, useState } from "react";
import ProtectedRoute from "@/components/auth/ProtectedRoute";
import { DeleteAccountModal } from "@/components/profile/DeleteAccountModal";
import { EditProfileModal } from "@/components/profile/EditProfileModal";
import { LogoutModal } from "@/components/profile/LogoutModal";
import ProfileTabs from "@/components/profile/ProfileTabs";
import { useAuth } from "@/context/AuthContext";
import { deleteCurrentUser, logout } from "@/lib/api";
import Link from "next/link";

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
  const [isLogoutOpen, setIsLogoutOpen] = useState(false);

  // Clears the session on the backend, resets the auth state,
  // then sends the user to the homepage
  async function handleLogout() {
    await logout();
    await refresh();
    router.push("/");
  }

  // Permanently deletes the account, clears the auth state,
  // then sends the user to the homepage
  async function handleDeleteAccount() {
    await deleteCurrentUser();
    await refresh();
    router.push("/");
  }

  // ProtectedRoute already guarantees a user exists,
  // but TypeScript still needs the narrowing
  if (!user) {
    return null;
  }

  // "Sanne de Vries" -> "SV"
  const initials = user.name
    .split(" ")
    .map((word) => word[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

  const joined = new Date(user.createdAt).toLocaleDateString("en-GB", {
    month: "short",
    year: "numeric",
  });

  return (
    <main className="mx-auto max-w-7xl px-6 py-8">
      <div className="mb-8 flex items-center gap-6 rounded-2xl border border-neutral-200 bg-white p-6">
        <div className="flex h-20 w-20 shrink-0 items-center justify-center rounded-full bg-purple-100 font-semibold text-2xl text-purple-700">
          {initials}
        </div>

        <div className="min-w-0 flex-1">
          <h1 className="font-bold text-3xl text-neutral-900">{user.name}</h1>
          <p className="mt-1 text-neutral-500">
            {user.email} · {user.location ?? "No location set"} · joined{" "}
            {joined}
          </p>
        </div>

        <button
          type="button"
          onClick={() => setIsEditing(true)}
          className="rounded-full border border-neutral-200 px-5 py-2 font-semibold text-sm hover:bg-neutral-50"
        >
          Edit profile
        </button>

        <button
          type="button"
          onClick={() => setIsLogoutOpen(true)}
          className="rounded-full border border-neutral-200 px-5 py-2 font-semibold text-sm hover:bg-neutral-50"
        >
          Log out
        </button>
      </div>

      {user.role === "admin" ? (
        <div className="rounded-2xl border border-neutral-200 bg-white p-6">
          <p className="text-neutral-600 text-sm">
            Saved and going events are for regular accounts. Manage events from
            the admin dashboard.
          </p>
          <Link
            href="/admin"
            className="mt-4 inline-block rounded-full bg-orange-600 px-5 py-2 font-semibold text-sm text-white hover:bg-orange-700"
          >
            Go to dashboard
          </Link>
        </div>
      ) : (
        <Suspense fallback={<p>Loading...</p>}>
          <ProfileTabs />
        </Suspense>
      )}

      <button
        type="button"
        onClick={() => setIsDeleteOpen(true)}
        className={`text-red-600 text-sm hover:underline ${user.role === "admin" ? "mt-8" : "mt-12"}`}
      >
        Delete account
      </button>

      {isEditing && (
        <EditProfileModal user={user} onClose={() => setIsEditing(false)} />
      )}

      {isDeleteOpen && (
        <DeleteAccountModal
          onConfirm={handleDeleteAccount}
          onClose={() => setIsDeleteOpen(false)}
        />
      )}

      {isLogoutOpen && (
        <LogoutModal
          onConfirm={handleLogout}
          onClose={() => setIsLogoutOpen(false)}
        />
      )}
    </main>
  );
}
