// Client wrapper: keeps Leaflet out of the server bundle entirely.

"use client";

import dynamic from "next/dynamic";

const EventMap = dynamic(() => import("./EventMap"), {
  ssr: false,
  loading: () => <div className="h-48 w-full rounded-xl bg-gray-100" />,
});

export default EventMap;
