// Confirmation for actions that cannot be undone.

interface ConfirmModalProps {
  title: string;
  message: string;
  confirmLabel: string;
  isBusy: boolean;
  onConfirm: () => void;
  onClose: () => void;
}

export default function ConfirmModal({
  title,
  message,
  confirmLabel,
  isBusy,
  onConfirm,
  onClose,
}: ConfirmModalProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-6">
        <h2 className="font-semibold text-xl">{title}</h2>
        <p className="mt-3 text-neutral-600 text-sm">{message}</p>

        <div className="mt-6 flex justify-end gap-3 border-t pt-5">
          <button
            type="button"
            disabled={isBusy}
            onClick={onClose}
            className="rounded-full border border-neutral-200 px-5 py-2 font-semibold text-sm hover:bg-neutral-50 disabled:opacity-50"
          >
            Keep it
          </button>
          <button
            type="button"
            disabled={isBusy}
            onClick={onConfirm}
            className="rounded-full bg-red-600 px-5 py-2 font-semibold text-red-50 text-sm hover:bg-red-700 disabled:opacity-50"
          >
            {isBusy ? "Working..." : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
