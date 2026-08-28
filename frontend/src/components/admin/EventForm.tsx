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

// Shared classes so every field looks the same
const LABEL = "block font-semibold text-sm";
const INPUT =
  "mt-2 w-full rounded-lg border border-neutral-200 px-3 py-2 outline-none focus:border-neutral-900";

// datetime-local wants "2026-09-13T21:00" — the ISO string without the timezone part
function toLocalInputValue(isoDate: string) {
  return isoDate.slice(0, 16);
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
    formEvent: { preventDefault: () => void; target: EventTarget | null },
    publishNow: boolean,
  ) {
    formEvent.preventDefault();
    setError("");

    if (selectedCategoryIds.length === 0) {
      setError("Please pick at least one category");
      return;
    }
    if (!image && !initialEvent) {
      setError("Please choose an image");
      return;
    }

    // Address comes from the newly picked location, or from the event being edited.
    // Both objects use the same field names, so we only build the list once.
    const source = location ?? initialEvent;
    // The backend requires a street, but a suggestion can come back without one
    if (!source || !source.street) {
      setError("Please pick a location with a street");
      return;
    }

    // The publish button is type="button", so currentTarget is the button,
    // not the form. Taking the form from the target works for both cases.
    const form = (formEvent.target as HTMLElement).closest("form");
    if (!form) return;
    const formData = new FormData(form);

    const address = {
      street: source.street,
      houseNumber: source.houseNumber,
      postalCode: source.postalCode,
      latitude: source.latitude,
      longitude: source.longitude,
      cityName: source.cityName,
      province: source.province,
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
      // Only clear when creating — when editing, the values should stay
      if (!initialEvent) {
        form.reset();
        setSelectedCategoryIds([]);
        setLocation(null);
        setImage(null);
      }
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
    <form className="space-y-6" onSubmit={(e) => handleSubmit(e, false)}>
      <label className={LABEL}>
        Title
        <input
          name="title"
          type="text"
          required
          maxLength={255}
          defaultValue={initialEvent?.title}
          className={INPUT}
        />
      </label>

      <label className={LABEL}>
        Description
        <textarea
          name="description"
          rows={4}
          defaultValue={initialEvent?.description ?? ""}
          className={`${INPUT} resize-y`}
        />
      </label>

      <fieldset>
        <legend className={LABEL}>Categories</legend>
        <div className="mt-3 flex flex-wrap gap-2">
          {categories.map((category) => {
            const isSelected = selectedCategoryIds.includes(category.id);
            return (
              <label
                key={category.id}
                className={`cursor-pointer rounded-full px-4 py-2 font-semibold text-sm ${
                  isSelected
                    ? "bg-neutral-900 text-white ring-2 ring-neutral-900 ring-offset-1"
                    : "border border-neutral-200 hover:bg-neutral-50"
                }`}
              >
                {/* The checkbox stays for keyboard and screen readers, just not visible */}
                <input
                  type="checkbox"
                  className="sr-only"
                  checked={isSelected}
                  onChange={() => toggleCategory(category.id)}
                />
                {category.name}
              </label>
            );
          })}
        </div>
      </fieldset>

      <div>
        <p className={LABEL}>Location</p>
        {initialEvent && !location && (
          <p className="mt-2 text-neutral-500 text-sm">
            Current: {initialEvent.street} {initialEvent.houseNumber},{" "}
            {initialEvent.cityName}
          </p>
        )}
        <div className="mt-2">
          <LocationAutocomplete
            onSelect={setLocation}
            placeholder="Search for an address"
            hint="Pick the exact address of the venue"
          />
        </div>
        {location && (
          <p className="mt-2 text-neutral-500 text-sm">
            Selected: {location.label}
          </p>
        )}
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <label className={LABEL}>
          Starts at
          <input
            name="startAt"
            type="datetime-local"
            required
            defaultValue={
              initialEvent ? toLocalInputValue(initialEvent.startAt) : undefined
            }
            className={INPUT}
          />
        </label>

        <label className={LABEL}>
          Ends at
          <input
            name="endAt"
            type="datetime-local"
            required
            defaultValue={
              initialEvent ? toLocalInputValue(initialEvent.endAt) : undefined
            }
            className={INPUT}
          />
        </label>
      </div>

      <label className={`${LABEL} sm:max-w-xs`}>
        Price (€)
        <input
          name="price"
          type="number"
          min="0"
          step="0.01"
          required
          defaultValue={initialEvent?.price ?? 0}
          className={INPUT}
        />
      </label>

      <div>
        <p className={LABEL}>Image</p>
        <div className="mt-2 flex items-center gap-4">
          <label className="cursor-pointer rounded-full border border-neutral-200 px-5 py-2 font-semibold text-sm hover:bg-neutral-50">
            Choose image
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              onChange={(e) => setImage(e.target.files?.[0] ?? null)}
              className="hidden"
            />
          </label>
          <span className="text-neutral-500 text-sm">
            {image ? image.name : "No image selected"}
          </span>
        </div>
      </div>

      {error && (
        <p className="rounded-lg bg-red-50 px-4 py-3 font-semibold text-red-700 text-sm">
          {error}
        </p>
      )}

      <div className="flex justify-end gap-3 border-t pt-5">
        <button
          type="submit"
          disabled={isSubmitting}
          className="rounded-full border border-neutral-200 px-5 py-2 font-semibold text-sm hover:bg-neutral-50 disabled:opacity-50"
        >
          {isSubmitting ? "Saving..." : "Save as draft"}
        </button>
        <button
          type="button"
          disabled={isSubmitting}
          onClick={(e) => handleSubmit(e, true)}
          className="rounded-full bg-neutral-900 px-5 py-2 font-semibold text-white text-sm hover:bg-neutral-800 disabled:opacity-50"
        >
          Publish now
        </button>
      </div>
    </form>
  );
}
