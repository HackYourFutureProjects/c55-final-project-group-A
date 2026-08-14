"use client";
import Image from "next/image";
import { useSearchParams } from "next/navigation";
import { type FormEvent, useState } from "react";

type AuthTab = "login" | "register";

export function AuthForm() {
  const searchParams = useSearchParams();
  const initialTab: AuthTab =
    searchParams.get("tab") === "register" ? "register" : "login";
  const [tab, setTab] = useState<AuthTab>(initialTab);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    // TODO: wire up to real auth endpoint once backend delivers it
    console.log(tab === "login" ? "login submit" : "register submit");
  }

  return (
    <div className="flex h-full overflow-hidden">
      <div className="flex w-full items-center justify-center px-8 sm:px-16 lg:w-2/5">
        <div className="w-full max-w-lg rounded-3xl border border-neutral-200 bg-white p-14 shadow-sm">
          <div className="mb-8 flex justify-center">
            <div className="inline-flex rounded-full bg-neutral-100 p-1">
              <button
                type="button"
                onClick={() => setTab("login")}
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
                onClick={() => setTab("register")}
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

          <form onSubmit={handleSubmit} className="flex flex-col gap-5">
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
              <div className="mb-1 flex items-center justify-between">
                <label
                  htmlFor="password"
                  className="block text-sm font-semibold"
                >
                  Password
                </label>
                {tab === "login"}
              </div>
              <input
                id="password"
                name="password"
                type="password"
                required
                placeholder="••••••••"
                className="w-full rounded-xl border border-neutral-200 bg-neutral-50 px-4 py-3 outline-none focus:border-orange-500"
              />
            </div>

            <button
              type="submit"
              className="mt-2 rounded-xl bg-orange-600 py-3 font-semibold text-white transition hover:bg-orange-700"
            >
              {tab === "login" ? "Log in" : "Create account"}
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
