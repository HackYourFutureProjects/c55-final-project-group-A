// Types for the admin event list and detail responses.
// Admin sees drafts and cancelled events, which the public API hides.

import type { Category } from "./event";

export interface AdminEventSummary {
  id: string;
  title: string;
  startAt: string;
  endAt: string;
  cityName: string;
  imageUrl: string | null;
  isPublished: boolean;
  cancelled: boolean;
}

export interface AdminEventPage {
  events: AdminEventSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface AdminEventDetail {
  id: string;
  title: string;
  description: string | null;
  categories: Category[];
  startAt: string;
  endAt: string;
  price: number;
  street: string;
  houseNumber: string | null;
  postalCode: string | null;
  cityName: string;
  province: string | null;
  latitude: number;
  longitude: number;
  imageUrl: string | null;
  goingCount: number;
  isPublished: boolean;
  cancelled: boolean;
}

// Status shown in the admin UI, derived from isPublished + cancelled + endAt.
export type AdminEventStatus = "DRAFT" | "PUBLISHED" | "CANCELLED" | "ENDED";
