"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { type SubmitEvent, useEffect, useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { resetPassword, validateResetToken } from "@/lib/api";

type Status = "checking" | "valid" | "invalid";

export function ResetPasswordForm() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token");
  const router = useRouter();
  const { refresh } = useAuth();

  const [status, setStatus] = useState<Status>("checking");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (!token) {
      setStatus("invalid");
      return;
    }

    validateResetToken(token)
      .then((result) => setStatus(result.valid ? "valid" : "invalid"))
      .catch(() => setStatus("invalid"));
  }, [token]);

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    if (newPassword !== confirmPassword) {
      setError("Passwords don't match.");
      return;
    }

    if (!token) {
      return;
    }

    setIsSubmitting(true);
    try {
      await resetPassword({ token, newPassword });
      await refresh();
      setSuccess(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong.");
    } finally {
      setIsSubmitting(false);
    }
  }

  if (status === "checking") {
    return (
      <div className="flex h-[calc(100vh-4rem)] items-center justify-center">
        <p className="text-neutral-500">Checking your link...</p>
      </div>
    );
  }

  if (status === "invalid") {
    return (
      <div className="flex h-[calc(100vh-4rem)] items-center justify-center px-8">
        <div className="w-full max-w-lg rounded-3xl border border-neutral-200 bg-white p-14 text-center shadow-sm">
          <h1 className="mb-2 text-3xl font-bold">Link expired</h1>
          <p className="text-neutral-500">
            This reset link is invalid or has expired. Please request a new one.
          </p>
        </div>
      </div>
    );
  }

  if (success) {
    return (
      <div className="flex h-[calc(100vh-4rem)] items-center justify-center px-8">
        <div className="w-full max-w-lg rounded-3xl border border-neutral-200 bg-white p-14 text-center shadow-sm">
          <h1 className="mb-2 text-3xl font-bold">Password changed</h1>
          <p className="mb-8 text-neutral-500">
            Your password has been updated and you're signed in.
          </p>
          <button
            type="button"
            onClick={() => router.push("/")}
            className="w-full rounded-xl bg-orange-600 px-4 py-3 font-semibold text-white transition hover:bg-orange-700"
          >
            Continue
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-[calc(100vh-4rem)] items-center justify-center px-8">
      <div className="w-full max-w-lg rounded-3xl border border-neutral-200 bg-white p-14 shadow-sm">
        <h1 className="mb-2 text-3xl font-bold">Reset your password</h1>
        <p className="mb-8 text-neutral-500">Enter a new password below.</p>

        <form onSubmit={handleSubmit} className="flex flex-col gap-5">
          <div>
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
              placeholder="••••••••"
              className="w-full rounded-xl border border-neutral-200 bg-neutral-50 px-4 py-3 outline-none focus:border-orange-500"
            />
            <p className="mt-1 text-sm text-neutral-500">8-30 characters</p>
          </div>

          <div>
            <label
              htmlFor="confirmPassword"
              className="mb-1 block text-sm font-semibold"
            >
              Confirm password
            </label>
            <input
              id="confirmPassword"
              type="password"
              required
              minLength={8}
              maxLength={30}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="••••••••"
              className="w-full rounded-xl border border-neutral-200 bg-neutral-50 px-4 py-3 outline-none focus:border-orange-500"
            />
          </div>

          {error && <p className="text-sm text-red-600">{error}</p>}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-xl bg-orange-600 px-4 py-3 font-semibold text-white transition hover:bg-orange-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? "Please wait..." : "Reset password"}
          </button>
        </form>
      </div>
    </div>
  );
}
