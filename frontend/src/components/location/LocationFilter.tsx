"use client";

import { useEffect, useState } from "react";
import { LocationAutocomplete } from "@/components/location/LocationAutocomplete";
import type { LocationSuggestion } from "@/types/location";

const RADIUS_OPTIONS = [25, 50, 100];

interface LocationFilterProps {
  latitude: number | null;
  longitude: number | null;
  radiusKm: number | null;
  onChange: (
    latitude: number | null,
    longitude: number | null,
    radiusKm: number | null,
  ) => void;
}

export default function LocationFilter({
  latitude,
  longitude,
  radiusKm,
  onChange,
}: LocationFilterProps) {
  // Bumped whenever the location disappears from the URL (e.g. via
  // the sidebar's "Clear all"), telling the search field to empty too
  const [clearCount, setClearCount] = useState(0);

  useEffect(() => {
    if (latitude === null) setClearCount((count) => count + 1);
  }, [latitude]);

  function handleSelect(suggestion: LocationSuggestion) {
    onChange(suggestion.latitude, suggestion.longitude, radiusKm ?? 10);
  }

  function handleRadiusChange(km: number) {
    if (latitude === null || longitude === null) return;
    onChange(latitude, longitude, km);
  }

  return (
    <div className="space-y-3">
      <LocationAutocomplete resetSignal={clearCount} onSelect={handleSelect} />

      <div className="flex flex-wrap gap-2">
        {RADIUS_OPTIONS.map((km) => (
          <button
            key={km}
            type="button"
            onClick={() => handleRadiusChange(km)}
            aria-pressed={radiusKm === km}
            disabled={latitude === null}
            className={`rounded-full px-3 py-1.5 text-[13px] transition-colors disabled:cursor-not-allowed disabled:opacity-40 ${
              radiusKm === km
                ? "bg-neutral-900 text-white"
                : "bg-neutral-100 text-neutral-600 hover:bg-neutral-200"
            }`}
          >
            {km} km
          </button>
        ))}
      </div>
    </div>
  );
}
