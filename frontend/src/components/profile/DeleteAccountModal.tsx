"use client";

import { useState } from "react";

interface DeleteAccountModalProps {
  onConfirm: () => Promise<void>;
  onClose: () => void;
}

export function DeleteAccountModal({
  onConfirm,
  onClose,
}: DeleteAccountModalProps) {
  const [error, setError] = useState<string | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Runs the deletion passed down from the profile page.
  // Keeps the modal open on failure so the user sees the error
  async function handleConfirm() {
    setError(null);
    setIsDeleting(true);

    try {
      await onConfirm();
    } catch {
      setError("Could not delete your account. Please try again.");
      setIsDeleting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-6">
        <h2 className="font-bold text-neutral-900 text-xl">
          Delete your account?
        </h2>
        <p className="mt-2 text-neutral-500 text-sm">This cannot be undone.</p>

        {error && <p className="mt-4 text-red-600 text-sm">{error}</p>}

        <div className="mt-6 flex justify-end gap-3 border-neutral-100 border-t pt-5">
          <button
            type="button"
            onClick={onClose}
            disabled={isDeleting}
            className="rounded-full border border-neutral-200 px-5 py-2 font-semibold text-sm hover:bg-neutral-50 disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={isDeleting}
            className="rounded-full bg-red-600 px-5 py-2 font-semibold text-red-50 text-sm hover:bg-red-700 disabled:opacity-50"
          >
            {isDeleting ? "Deleting..." : "Delete account"}
          </button>
        </div>
      </div>
    </div>
  );
}
