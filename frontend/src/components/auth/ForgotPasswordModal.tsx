"use client";

import { useState } from "react";
import { forgotPassword } from "@/lib/api";

interface ForgotPasswordModalProps {
  onClose: () => void;
}

export function ForgotPasswordModal({ onClose }: ForgotPasswordModalProps) {
  const [email, setEmail] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setIsSubmitting(true);

    try {
      await forgotPassword({ email });
    } catch {
    } finally {
      setIsSubmitting(false);
      setSubmitted(true);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-6">
        {submitted ? (
          <>
            <h2 className="font-bold text-neutral-900 text-xl">
              Check your email
            </h2>
            <p className="mt-2 text-neutral-500 text-sm">
              If this email is registered, a reset link has been sent.
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
              Forgot your password?
            </h2>
            <p className="mt-2 text-neutral-500 text-sm">
              Enter your email and we'll send you a reset link.
            </p>

            <form onSubmit={handleSubmit}>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Email"
                className="mt-4 w-full rounded-lg border border-neutral-200 px-4 py-2 text-sm focus:border-orange-500 focus:outline-none"
              />

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
                  {isSubmitting ? "Sending..." : "Send reset link"}
                </button>
              </div>
            </form>
          </>
        )}
      </div>
    </div>
  );
}
