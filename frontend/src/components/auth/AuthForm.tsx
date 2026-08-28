"use client";
import Image from "next/image";
import { useRouter, useSearchParams } from "next/navigation";
import { type SubmitEvent, useRef, useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { login, register } from "@/lib/api";

type AuthTab = "login" | "register";

export function AuthForm() {
  const searchParams = useSearchParams();
  const initialTab: AuthTab =
    searchParams.get("tab") === "register" ? "register" : "login";
  const [tab, setTab] = useState<AuthTab>(initialTab);

  const router = useRouter();
  const { refresh } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const formRef = useRef<HTMLFormElement>(null);

  function switchTab(nextTab: AuthTab) {
    setTab(nextTab);
    setError(null);
    formRef.current?.reset();
  }

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);

    const formData = new FormData(event.currentTarget);
    const email = formData.get("email") as string;
    const password = formData.get("password") as string;

    try {
      if (tab === "login") {
        await login({ email, password });
      } else {
        const name = formData.get("name") as string;
        await register({ name, email, password });
      }
      const currentUser = await refresh();
      router.push(currentUser?.role === "admin" ? "/admin" : "/profile");
    } catch {
      setError(
        "Something went wrong. Please check your details and try again.",
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="flex h-[calc(100vh-4rem)] overflow-hidden">
      <div className="flex w-full items-center justify-center px-8 sm:px-16 lg:w-2/5">
        <div className="w-full max-w-lg rounded-3xl border border-neutral-200 bg-white p-14 shadow-sm">
          <div className="mb-8 flex justify-center">
            <div className="inline-flex rounded-full bg-neutral-100 p-1">
              <button
                type="button"
                onClick={() => switchTab("login")}
                className={`rounded-full px-5 py-2 text-sm font-semibold transition ${
                  tab === "login"
                    ? "bg-orange-600 shadow text-white"
                    : "text-neutral-500"
                }`}
              >
                Log in
              </button>
              <button
                type="button"
                onClick={() => switchTab("register")}
                className={`rounded-full px-5 py-2 text-sm font-semibold transition ${
                  tab === "register"
                    ? "bg-orange-600 shadow text-white"
                    : "text-neutral-500"
                }`}
              >
                Register
              </button>
            </div>
          </div>

          {tab === "login" ? (
            <>
              <h1 className="mb-2 text-4xl font-bold">Welcome back</h1>
              <p className="mb-8 text-neutral-500">
                Your saved events and reminders are waiting.
              </p>
            </>
          ) : (
            <>
              <h1 className="mb-2 text-4xl font-bold">Create account</h1>
              <p className="mb-8 text-neutral-500">
                Find events happening near you.
              </p>
            </>
          )}

          <form
            ref={formRef}
            onSubmit={handleSubmit}
            className="flex flex-col gap-5"
          >
            {tab === "register" && (
              <div>
                <label
                  htmlFor="name"
                  className="mb-1 block text-sm font-semibold"
                >
                  Name
                </label>
                <input
                  id="name"
                  name="name"
                  type="text"
                  required
                  minLength={2}
                  placeholder="Your name"
                  className="w-full rounded-xl border border-neutral-200 bg-neutral-50 px-4 py-3 outline-none focus:border-orange-500"
                />
              </div>
            )}

            <div>
              <label
                htmlFor="email"
                className="mb-1 block text-sm font-semibold"
              >
                Email
              </label>
              <input
                id="email"
                name="email"
                type="email"
                required
                placeholder="you@email.com"
                className="w-full rounded-xl border border-neutral-200 bg-neutral-50 px-4 py-3 outline-none focus:border-orange-500"
              />
            </div>

            <div>
              <div>
                <label
                  htmlFor="password"
                  className="mb-1 block text-sm font-semibold"
                >
                  Password
                </label>
                <input
                  id="password"
                  name="password"
                  type="password"
                  required
                  minLength={tab === "register" ? 8 : undefined}
                  placeholder="••••••••"
                  className="w-full rounded-xl border border-neutral-200 bg-neutral-50 px-4 py-3 outline-none focus:border-orange-500"
                />
                {tab === "register" && (
                  <p className="mt-1 text-sm text-neutral-500">
                    At least 8 characters
                  </p>
                )}
              </div>
            </div>
            {error && <p className="text-sm text-red-600">{error}</p>}
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full rounded-xl bg-orange-600 px-4 py-3 font-semibold text-white transition hover:bg-orange-700 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isSubmitting
                ? "Please wait..."
                : tab === "login"
                  ? "Log in"
                  : "Sign up"}
            </button>
          </form>
        </div>
      </div>

      <div className="relative hidden overflow-hidden lg:block lg:w-3/5">
        <Image
          src="/login.png"
          alt="People enjoying an event"
          fill
          className="object-cover"
          priority
        />
      </div>
    </div>
  );
}
