export interface Category {
  id: string;
  name: string;
}
export interface Event {
  id: string;
  title: string;
  categories: Category[];
  startAt: string;
  endAt: string;
  price: number;
  street: string;
  houseNumber: string | null;
  postalCode: string | null;
  cityName: string;
  province: string | null;
  imageUrl: string | null;
  goingCount?: number;
  cancelled: boolean;
  latitude: number;
  longitude: number;
}

export interface EventPage {
  events: Event[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export type EventStatus = "UPCOMING" | "ONGOING" | "PAST" | "CANCELLED";

export interface EventDetail {
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
  imageUrl: string;
  goingCount: number;
  eventStatus: EventStatus;
  latitude: number;
  longitude: number;
}

export interface EventFilters {
  page?: number;
  size?: number;
  search?: string;
  categoryIds?: string[];
  latitude?: number;
  longitude?: number;
  radiusKm?: number;
  dateFrom?: string;
  dateTo?: string;
  price?: PriceFilter;
  timesOfDay?: TimeOfDay[]; // repeated key, like categoryIds
}

export type PriceFilter = "FREE" | "PAID";

export type TimeOfDay = "MORNING" | "AFTERNOON" | "EVENING";
