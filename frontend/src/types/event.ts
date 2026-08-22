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
  goingCount: number;
  eventStatus: EventStatus;
}