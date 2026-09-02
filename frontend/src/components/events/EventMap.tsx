// Leaflet map showing a single pin at the event's location.
// Loaded only in the browser via EventMapClient — Leaflet touches window on import.

"use client";

import L from "leaflet";
import { MapContainer, Marker, TileLayer } from "react-leaflet";
import "leaflet/dist/leaflet.css";

// Leaflet's default marker images break in bundlers, so we load them from a CDN.
const icon = L.icon({
  iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  iconRetinaUrl:
    "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
  iconSize: [25, 41],
  iconAnchor: [12, 41],
});

interface EventMapProps {
  latitude: number;
  longitude: number;
  title: string;
}

export default function EventMap({
  latitude,
  longitude,
  title,
}: EventMapProps) {
  return (
    <MapContainer
      center={[latitude, longitude]}
      zoom={15}
      scrollWheelZoom={false} // don't hijack page scrolling
      className="h-48 w-full rounded-xl"
    >
      {/* Free OpenStreetMap tiles — attribution is required by their license. */}
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <Marker position={[latitude, longitude]} icon={icon} title={title} />
    </MapContainer>
  );
}
