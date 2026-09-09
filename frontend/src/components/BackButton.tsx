"use client";

import { useRouter } from "next/navigation";

export default function BackButton() {
  const router = useRouter();

  return (
    <button
      type="button"
      onClick={() => router.back()}
      className="mb-3 inline-flex items-center gap-1.5 text-sm font-medium text-gray-600 transition-colors hover:text-orange-600 hover:underline underline-offset-4"
    >
      ← Back
    </button>
  );
}
