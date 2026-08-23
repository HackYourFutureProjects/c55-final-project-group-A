export interface LocationSuggestion {
  id: string;
  label: string;
  street: string | null;
  houseNumber: string | null;
  postalCode: string | null;
  latitude: number;
  longitude: number;
  cityName: string;
  province: string | null;
}
