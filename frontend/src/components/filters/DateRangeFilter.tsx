"use client";

import { useState } from "react";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";

interface DateRangeFilterProps {
  // Current values from the URL, as YYYY-MM-DD strings
  dateFrom: string | null;
  dateTo: string | null;
  onChange: (dateFrom: string | null, dateTo: string | null) => void;
}

// Builds YYYY-MM-DD from local parts. toISOString() would convert to UTC
// and shift the day by one in the Amsterdam timezone
function toDateParam(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

// "2026-09-12" -> Date at local midnight
function toDate(value: string | null): Date | null {
  return value ? new Date(`${value}T00:00:00`) : null;
}

export default function DateRangeFilter({
  dateFrom,
  dateTo,
  onChange,
}: DateRangeFilterProps) {
  // The picker reports the start date before the end date is chosen,
  // but the backend needs both — so the half-picked range lives here
  const [range, setRange] = useState<[Date | null, Date | null]>([
    toDate(dateFrom),
    toDate(dateTo),
  ]);

  const [start, end] = range;

  function handleChange(dates: [Date | null, Date | null]) {
    setRange(dates);

    const [newStart, newEnd] = dates;

    // Only touch the URL once the range is complete, or when it is cleared
    if (newStart && newEnd) {
      onChange(toDateParam(newStart), toDateParam(newEnd));
    } else if (!newStart && !newEnd) {
      onChange(null, null);
    }
  }

  return (
    <DatePicker
      selectsRange
      startDate={start ?? undefined}
      endDate={end ?? undefined}
      onChange={handleChange}
      minDate={new Date()}
      isClearable
      placeholderText="Pick a date range"
      dateFormat="d MMM"
      popperPlacement="bottom-start"
      wrapperClassName="w-full"
      popperClassName="z-50"
      className="w-full rounded-xl bg-neutral-100 px-4 py-2 text-sm outline-none placeholder:text-neutral-400"
    />
  );
}
