interface HomeBannerProps {
  eventCount: number;
}

const chipBase = "rounded-full px-4 py-1.5 text-sm font-semibold";

export default function HomeBanner({ eventCount }: HomeBannerProps) {
  const today = new Date();
  const formattedDate = today.toLocaleDateString("en-GB", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });

  return (
    <div className="relative overflow-hidden rounded-3xl bg-orange-50 px-8 py-10">
      <div
        aria-hidden
        className="pointer-events-none absolute -top-10 right-0 flex gap-3"
      >
        <div className="h-40 w-14 rotate-12 rounded-2xl bg-purple-200" />
        <div className="h-48 w-14 rotate-12 rounded-2xl bg-amber-200" />
        <div className="h-40 w-14 rotate-12 rounded-2xl bg-blue-200" />
        <div className="h-48 w-14 rotate-12 rounded-2xl bg-green-200" />
        <div className="h-40 w-14 rotate-12 rounded-2xl bg-pink-200" />
      </div>

      <div className="relative flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <div className="mb-6 flex flex-wrap gap-2">
            <span className={`${chipBase} bg-purple-100 text-purple-700`}>
              🎵 Music
            </span>
            <span className={`${chipBase} bg-orange-100 text-orange-700`}>
              🎨 Art
            </span>
            <span className={`${chipBase} bg-blue-100 text-blue-700`}>
              ⚽ Sports
            </span>
            <span className={`${chipBase} bg-amber-100 text-amber-700`}>
              🍔 Food
            </span>
            <span className={`${chipBase} bg-pink-100 text-pink-700`}>
              🎭 Theatre
            </span>
          </div>

          <h1 className="text-4xl font-bold tracking-tight sm:text-5xl">
            What's on in{" "}
            <span className="text-orange-600">the Netherlands</span>
          </h1>

          <p className="mt-3 text-neutral-500">
            {formattedDate} · {eventCount} events
          </p>
        </div>
      </div>
    </div>
  );
}
