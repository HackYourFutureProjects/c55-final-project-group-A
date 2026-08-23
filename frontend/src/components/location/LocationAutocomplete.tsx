"use client";

import { useEffect, useState } from "react";
import { getLocationSuggestions } from "@/lib/api";
import type { LocationSuggestion } from "@/types/location";

interface LocationAutocompleteProps {
  onSelect: (suggestion: LocationSuggestion) => void;
  placeholder?: string;
  hint?: string;
}

// Debounced location search. The parent decides what to do with the
// selected suggestion — the homepage filter only needs the coordinates,
// the admin form sends the whole object
export function LocationAutocomplete({
  onSelect,
  placeholder,
   hint,
}: LocationAutocompleteProps) {
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState<LocationSuggestion[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isFocused, setIsFocused] = useState(false);
  useEffect(() => {
    // Too short to be a meaningful search — the geocoder needs
    // complete words, so one or two letters return noise
    if (query.length < 3) {
      setSuggestions([]);
      return;
    }

    setIsLoading(true);

    const timer = setTimeout(() => {
      getLocationSuggestions(query)
        .then(setSuggestions)
        // A failed lookup (503 from the geocoder, bad query) just
        // means no suggestions — not an error worth showing
        .catch(() => setSuggestions([]))
        .finally(() => setIsLoading(false));
    }, 600);

    // Runs before the next effect: cancels the pending request so we
    // only search once the user pauses typing. 600ms rather than the
    // usual 300 because the geocoder doesn't match partial words
    return () => clearTimeout(timer);
  }, [query]);

  // Fills the input with the chosen label, closes the list,
  // and hands the full suggestion to the parent
  function handleSelect(suggestion: LocationSuggestion) {
    setQuery(suggestion.label);
    setSuggestions([]);
    onSelect(suggestion);
  }

  return (
    <div>
      <input
        type="text"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        onFocus={() => setIsFocused(true)}
        onBlur={() => setIsFocused(false)}
        placeholder={placeholder}
      />
      {/* Shown while the field is focused but the query is too short
    for the geocoder to return anything useful */}
      {isFocused && query.length < 3 && (
  <p>{hint ?? "Type at least 3 characters"}</p>
)}


      {isLoading && <p>Searching...</p>}

      {/* No "no results" message — the geocoder often returns nothing
          mid-word, so an empty list is normal while typing */}
      {suggestions.length > 0 && (
        <ul>
          {suggestions.map((suggestion) => (
            <li key={suggestion.id}>
              <button type="button" onClick={() => handleSelect(suggestion)}>
                {suggestion.label}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
