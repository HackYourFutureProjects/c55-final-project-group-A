"use client";

import { useState } from "react";
import { changePassword } from "@/lib/api";

interface ChangePasswordModalProps {
  onClose: () => void;
}

export function ChangePasswordModal({ onClose }: ChangePasswordModalProps) {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      await changePassword({ currentPassword, newPassword });
      setSuccess(true);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Could not change your password. Please try again.",
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-6">
        {success ? (
          <>
            <h2 className="font-bold text-neutral-900 text-xl">
              Password changed
            </h2>
            <p className="mt-2 text-neutral-500 text-sm">
              Your password has been updated. Your other devices have been
              signed out.
            </p>
            <div className="mt-6 flex justify-end border-neutral-100 border-t pt-5">
              <button
                type="button"
                onClick={onClose}
                className="rounded-full bg-orange-600 px-5 py-2 font-semibold text-orange-50 text-sm hover:bg-orange-700"
              >
                Close
              </button>
            </div>
          </>
        ) : (
          <>
            <h2 className="font-bold text-neutral-900 text-xl">
              Change password
            </h2>
            <p className="mt-2 text-neutral-500 text-sm">
              Enter your current password and choose a new one.
            </p>

            <form onSubmit={handleSubmit}>
              <div className="mt-4">
                <label
                  htmlFor="currentPassword"
                  className="mb-1 block text-sm font-semibold"
                >
                  Current password
                </label>
                <input
                  id="currentPassword"
                  type="password"
                  required
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  className="w-full rounded-lg border border-neutral-200 px-4 py-2 text-sm focus:border-orange-500 focus:outline-none"
                />
              </div>

              <div className="mt-4">
                <label
                  htmlFor="newPassword"
                  className="mb-1 block text-sm font-semibold"
                >
                  New password
                </label>
                <input
                  id="newPassword"
                  type="password"
                  required
                  minLength={8}
                  maxLength={30}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  className="w-full rounded-lg border border-neutral-200 px-4 py-2 text-sm focus:border-orange-500 focus:outline-none"
                />
                <p className="mt-1 text-sm text-neutral-500">8-30 characters</p>
              </div>

              {error && <p className="mt-3 text-red-600 text-sm">{error}</p>}

              <div className="mt-6 flex justify-end gap-3 border-neutral-100 border-t pt-5">
                <button
                  type="button"
                  onClick={onClose}
                  disabled={isSubmitting}
                  className="rounded-full border border-neutral-200 px-5 py-2 font-semibold text-sm hover:bg-neutral-50 disabled:opacity-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="rounded-full bg-orange-600 px-5 py-2 font-semibold text-orange-50 text-sm hover:bg-orange-700 disabled:opacity-50"
                >
                  {isSubmitting ? "Saving..." : "Change password"}
                </button>
              </div>
            </form>
          </>
        )}
      </div>
    </div>
  );
}
