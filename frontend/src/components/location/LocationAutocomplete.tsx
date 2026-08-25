"use client";

import { useEffect, useRef, useState } from "react";
import { getLocationSuggestions } from "@/lib/api";
import type { LocationSuggestion } from "@/types/location";

interface LocationAutocompleteProps {
  onSelect: (suggestion: LocationSuggestion) => void;
  placeholder?: string;
  resetSignal?: number;
}

// One value instead of three separate booleans, so the UI can never
// show a stale combination (e.g. "no results" while still typing)
type Status =
  | "idle"
  | "typing"
  | "searching"
  | "results"
  | "no-results"
  | "service-unavailable"
  | "selected";

export function LocationAutocomplete({
  onSelect,
  placeholder,
  resetSignal,
}: LocationAutocompleteProps) {
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState<LocationSuggestion[]>([]);
  const [status, setStatus] = useState<Status>("idle");

  // biome-ignore lint/correctness/useExhaustiveDependencies: resetSignal is a counter used only to trigger this effect
  useEffect(() => {
    setQuery("");
    setSuggestions([]);
    setStatus("idle");
  }, [resetSignal]);

  const justSelectedRef = useRef(false);
  const lastRequestTimeRef = useRef(0);
  useEffect(() => {
    if (justSelectedRef.current) {
      justSelectedRef.current = false;
      return;
    }

    if (query.length < 3) {
      setSuggestions([]);
      setStatus("idle");
      return;
    }

    setStatus("searching");

    const timer = setTimeout(() => {
      getLocationSuggestions(query)
        .then((results) => {
          setSuggestions(results);
          setStatus(results.length > 0 ? "results" : "no-results");
        })
        .catch((error) => {
          setSuggestions([]);
          // 503 means the geocoder is temporarily rate-limited —
          // different message than "no matching city"
          const isUnavailable =
            error instanceof Error && error.message.includes("503");
          setStatus(isUnavailable ? "service-unavailable" : "no-results");
        });
    }, 900);

    return () => clearTimeout(timer);
  }, [query]);

  function handleChange(value: string) {
    setQuery(value);
    // Marks the field as "typing" the instant a key is pressed, in the
    // same render — no frame where the old status is still visible
    setStatus(value.length < 3 ? "idle" : "typing");
  }

  function handleSelect(suggestion: LocationSuggestion) {
    justSelectedRef.current = true;
    setQuery(suggestion.label);
    setSuggestions([]);
    setStatus("selected");
    onSelect(suggestion);
  }

  return (
    <div>
      <div className="relative">
        <input
          type="text"
          value={query}
          onChange={(event) => handleChange(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") event.preventDefault();
          }}
          placeholder={placeholder}
          className="w-full rounded-xl border border-neutral-200 bg-neutral-50 px-4 py-2 text-sm outline-none focus:border-orange-300 focus:bg-white"
        />

        {status === "results" && (
          <ul className="absolute top-full z-20 mt-1 w-full rounded-xl border border-neutral-200 bg-white py-1 shadow-lg">
            {suggestions.map((suggestion) => (
              <li key={suggestion.id}>
                <button
                  type="button"
                  onClick={() => handleSelect(suggestion)}
                  className="w-full px-4 py-2 text-left text-sm hover:bg-orange-50"
                >
                  {suggestion.label}
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      <p className="mt-1.5 text-neutral-400 text-xs">
        Enter the full city name and pick a radius
      </p>

      {status === "searching" && (
        <p className="mt-1 text-neutral-400 text-xs">Searching...</p>
      )}

      {status === "no-results" && (
        <p className="mt-1 text-neutral-400 text-xs">
          No matching city. Try the full name.
        </p>
      )}
      {status === "service-unavailable" && (
        <p className="mt-1 text-neutral-400 text-xs">
          Search is temporarily busy. Try again in a moment.
        </p>
      )}
    </div>
  );
}
