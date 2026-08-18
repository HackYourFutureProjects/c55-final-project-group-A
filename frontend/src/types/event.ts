export interface Event {
  id: string;
  title: string;
  categoryName: string;
  startAt: string;
  endAt: string;
  price: number;
  street: string;
  houseNumber: string;
  postalCode: string;
  cityName: string;
  province: string;
  imageUrl: string | null;
  goingCount: number;
  cancelled: boolean;
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
  categoryName: string;
  startAt: string;
  endAt: string;
  price: number;
  street: string;
  houseNumber: string;
  postalCode: string;
  cityName: string;
  province: string;
  imageUrl: string | null;
  goingCount: number;
  eventStatus: EventStatus;
}
