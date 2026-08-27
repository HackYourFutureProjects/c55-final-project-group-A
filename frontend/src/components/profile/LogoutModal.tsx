"use client";

interface LogoutModalProps {
  onConfirm: () => void;
  onClose: () => void;
}

export function LogoutModal({ onConfirm, onClose }: LogoutModalProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-6">
        <h2 className="font-bold text-neutral-900 text-xl">Log out?</h2>
        <p className="mt-2 text-neutral-500 text-sm">
          You will need to log in again to see your saved events.
        </p>

        <div className="mt-6 flex justify-end gap-3 border-neutral-100 border-t pt-5">
          <button
            type="button"
            onClick={onClose}
            className="rounded-full border border-neutral-200 px-5 py-2 font-semibold text-sm hover:bg-neutral-50"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            className="rounded-full bg-neutral-900 px-5 py-2 font-semibold text-sm text-white hover:bg-neutral-800"
          >
            Log out
          </button>
        </div>
      </div>
    </div>
  );
}
