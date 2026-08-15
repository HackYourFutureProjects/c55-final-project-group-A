export interface Event {
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
  cancelled: boolean;
}
