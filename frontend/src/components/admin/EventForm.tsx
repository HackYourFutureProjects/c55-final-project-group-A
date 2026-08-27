// Shared form for creating and editing an event.

"use client";

import { type SubmitEvent, useEffect, useState } from "react";
import { LocationAutocomplete } from "@/components/location/LocationAutocomplete";
import { getCategories } from "@/lib/api";
import type { AdminEventDetail, CreateEventRequest } from "@/types/admin";
import type { Category } from "@/types/event";
import type { LocationSuggestion } from "@/types/location";

interface EventFormProps {
  initialEvent?: AdminEventDetail;
  onSubmit: (
    event: CreateEventRequest,
    image: File | null,
    publishNow: boolean,
  ) => Promise<void>;
}

// Turns an ISO string into the value a datetime-local input expects
// ("2026-09-13T21:00"), in the browser's own timezone.
function toLocalInputValue(isoDate: string) {
  const date = new Date(isoDate);
  const offsetInMs = date.getTimezoneOffset() * 60 * 1000;
  return new Date(date.getTime() - offsetInMs).toISOString().slice(0, 16);
}

export default function EventForm({ initialEvent, onSubmit }: EventFormProps) {
  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCategoryIds, setSelectedCategoryIds] = useState<string[]>(
    initialEvent?.categories.map((category) => category.id) ?? [],
  );
  const [location, setLocation] = useState<LocationSuggestion | null>(null);
  const [image, setImage] = useState<File | null>(null);
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    getCategories()
      .then(setCategories)
      .catch(() => setCategories([]));
  }, []);

  function toggleCategory(categoryId: string) {
    setSelectedCategoryIds((current) =>
      current.includes(categoryId)
        ? current.filter((id) => id !== categoryId)
        : [...current, categoryId],
    );
  }

  async function handleSubmit(
    formEvent: SubmitEvent<HTMLFormElement>,
    publishNow: boolean,
  ) {
    formEvent.preventDefault();
    setError("");

    if (selectedCategoryIds.length === 0) {
      setError("Please pick at least one category");
      return;
    }
    // No initialEvent means "create", initialEvent means "edit".
    // When editing we keep the existing address unless a new one is picked
    if (!location && !initialEvent) {
      setError("Please pick a location");
      return;
    }
    // The backend requires a street, but a suggestion can come back without one
    if (location && !location.street) {
      setError("This location has no street. Pick a more precise address.");
      return;
    }
    if (!image && !initialEvent) {
      setError("Please choose an image");
      return;
    }

    const formData = new FormData(formEvent.currentTarget);

    // Either the freshly picked location, or the one the event already has
    const address = location
      ? {
          street: location.street as string,
          houseNumber: location.houseNumber,
          postalCode: location.postalCode,
          latitude: location.latitude,
          longitude: location.longitude,
          cityName: location.cityName,
          province: location.province,
        }
      : {
          street: initialEvent!.street,
          houseNumber: initialEvent!.houseNumber,
          postalCode: initialEvent!.postalCode,
          latitude: initialEvent!.latitude,
          longitude: initialEvent!.longitude,
          cityName: initialEvent!.cityName,
          province: initialEvent!.province,
        };

    setIsSubmitting(true);
    try {
      await onSubmit(
        {
          title: String(formData.get("title")),
          description: String(formData.get("description")) || null,
          categoryIds: selectedCategoryIds,
          address,
          // datetime-local has no timezone, so we convert to a full ISO string
          startAt: new Date(String(formData.get("startAt"))).toISOString(),
          endAt: new Date(String(formData.get("endAt"))).toISOString(),
          price: Number(formData.get("price")),
        },
        image,
        publishNow,
      );
    } catch (submitError) {
      setError(
        submitError instanceof Error
          ? submitError.message
          : "Something went wrong",
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form onSubmit={(e) => handleSubmit(e, false)}>
      <label>
        Title
        <input
          name="title"
          type="text"
          required
          maxLength={255}
          defaultValue={initialEvent?.title}
        />
      </label>

      <label>
        Description
        <textarea
          name="description"
          defaultValue={initialEvent?.description ?? ""}
        />
      </label>

      <fieldset>
        <legend>Categories</legend>
        {categories.map((category) => (
          <label key={category.id}>
            <input
              type="checkbox"
              checked={selectedCategoryIds.includes(category.id)}
              onChange={() => toggleCategory(category.id)}
            />
            {category.name}
          </label>
        ))}
      </fieldset>

      <div>
        <p>Location</p>
        {initialEvent && !location && (
          <p>
            Current: {initialEvent.street} {initialEvent.houseNumber},{" "}
            {initialEvent.cityName}
          </p>
        )}
        <LocationAutocomplete
          onSelect={setLocation}
          placeholder="Search for an address"
          hint="Pick the exact address of the venue"
        />
        {location && <p>Selected: {location.label}</p>}
      </div>

      <label>
        Starts at
        <input
          name="startAt"
          type="datetime-local"
          required
          defaultValue={
            initialEvent ? toLocalInputValue(initialEvent.startAt) : undefined
          }
        />
      </label>

      <label>
        Ends at
        <input
          name="endAt"
          type="datetime-local"
          required
          defaultValue={
            initialEvent ? toLocalInputValue(initialEvent.endAt) : undefined
          }
        />
      </label>

      <label>
        Price (€)
        <input
          name="price"
          type="number"
          min="0"
          step="0.01"
          required
          defaultValue={initialEvent?.price ?? 0}
        />
      </label>

      <label>
        Image
        <input
          type="file"
          accept="image/jpeg,image/png,image/webp"
          onChange={(e) => setImage(e.target.files?.[0] ?? null)}
        />
      </label>

      {error && <p>{error}</p>}

      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Saving..." : "Save as draft"}
      </button>
      <button
        type="button"
        disabled={isSubmitting}
        onClick={(e) => handleSubmit(e as never, true)}
      >
        Publish now
      </button>
    </form>
  );
}
