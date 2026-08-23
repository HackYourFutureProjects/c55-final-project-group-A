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
    <div>
      <h2>Delete your account?</h2>
      <p>This cannot be undone.</p>

      {error && <p>{error}</p>}

      <button type="button" onClick={onClose} disabled={isDeleting}>
        Cancel
      </button>
      <button type="button" onClick={handleConfirm} disabled={isDeleting}>
        {isDeleting ? "Deleting..." : "Delete account"}
      </button>
    </div>
  );
}
