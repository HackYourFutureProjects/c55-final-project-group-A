"use client";

import { useEffect, useState } from "react";
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

  useEffect(() => {
    // "selected" is set directly in handleSelect — this effect only
    // reacts to further typing, not to the value it just wrote itself
    if (status === "selected") return;

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
        .catch(() => {
          setSuggestions([]);
          setStatus("no-results");
        });
    }, 700);

    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query, status]);

  function handleChange(value: string) {
    setQuery(value);
    // Marks the field as "typing" the instant a key is pressed, in the
    // same render — no frame where the old status is still visible
    setStatus(value.length < 3 ? "idle" : "typing");
  }

  function handleSelect(suggestion: LocationSuggestion) {
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
    </div>
  );
}
