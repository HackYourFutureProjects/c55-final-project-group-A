"use client";

import { type SubmitEvent, useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { updateCurrentUser } from "@/lib/api";
import type { User } from "@/types/user";

interface EditProfileModalProps {
  user: User;
  onClose: () => void;
}

export function EditProfileModal({ user, onClose }: EditProfileModalProps) {
  const { refresh } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Sends the edited fields, re-reads the user so the profile
  // and navbar update, then closes the modal
  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);

    const formData = new FormData(event.currentTarget);

    try {
      await updateCurrentUser({
        name: formData.get("name") as string,
        email: formData.get("email") as string,
        location: formData.get("location") as string,
      });
      await refresh();
      onClose();
    } catch {
      setError("Could not save your changes. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-6">
        <h2 className="font-bold text-neutral-900 text-xl">Edit profile</h2>

        <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-4">
          <div>
            <label
              htmlFor="name"
              className="mb-1 block font-medium text-neutral-600 text-sm"
            >
              Name
            </label>
            <input
              id="name"
              name="name"
              type="text"
              required
              minLength={2}
              defaultValue={user.name}
              className="w-full rounded-lg border border-neutral-200 px-3 py-2 outline-none focus:border-neutral-900"
            />
          </div>

          <div>
            <label
              htmlFor="email"
              className="mb-1 block font-medium text-neutral-600 text-sm"
            >
              Email
            </label>
            <input
              id="email"
              name="email"
              type="email"
              required
              defaultValue={user.email}
              className="w-full rounded-lg border border-neutral-200 px-3 py-2 outline-none focus:border-neutral-900"
            />
          </div>

          <div>
            <label
              htmlFor="location"
              className="mb-1 block font-medium text-neutral-600 text-sm"
            >
              Location
            </label>
            <input
              id="location"
              name="location"
              type="text"
              defaultValue={user.location ?? ""}
              className="w-full rounded-lg border border-neutral-200 px-3 py-2 outline-none focus:border-neutral-900"
            />
          </div>

          {error && <p className="text-red-600 text-sm">{error}</p>}

          <div className="mt-2 flex justify-end gap-3 border-neutral-100 border-t pt-5">
            <button
              className="rounded-full border border-neutral-200 px-5 py-2 font-semibold text-sm hover:bg-neutral-50"
              type="button"
              onClick={onClose}
            >
              Cancel
            </button>
            <button
              className="rounded-full bg-neutral-900 px-5 py-2 font-semibold text-sm text-white hover:bg-neutral-800 disabled:opacity-50"
              type="submit"
              disabled={isSubmitting}
            >
              {isSubmitting ? "Saving..." : "Save"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
