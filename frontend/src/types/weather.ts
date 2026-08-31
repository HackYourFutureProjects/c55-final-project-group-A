export interface Weather {
  isAvailable: boolean;
  temperature: number | null;
  condition: string | null;
  precipitationChance: number | null;
  windSpeed: number | null;
}
