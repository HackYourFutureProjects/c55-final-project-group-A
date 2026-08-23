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
    <div>
      <h2>Edit profile</h2>

      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="name">Name</label>
          <input
            id="name"
            name="name"
            type="text"
            required
            minLength={2}
            defaultValue={user.name}
          />
        </div>

        <div>
          <label htmlFor="email">Email</label>
          <input
            id="email"
            name="email"
            type="email"
            required
            defaultValue={user.email}
          />
        </div>

        <div>
          <label htmlFor="location">Location</label>
          <input
            id="location"
            name="location"
            type="text"
            defaultValue={user.location ?? ""}
          />
        </div>

        {error && <p>{error}</p>}

        <button type="button" onClick={onClose}>
          Cancel
        </button>
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Saving..." : "Save"}
        </button>
      </form>
    </div>
  );
}
