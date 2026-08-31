import { getWeather } from "@/lib/api";

type Props = {
  latitude: number;
  longitude: number;
  startAt: string;
};

// background hint that matches the condition
function getWeatherStyle(condition: string | null) {
  const text = condition?.toLowerCase() ?? "";

  if (text.includes("rain") || text.includes("drizzle")) {
    return "from-slate-50 to-blue-50";
  }
  if (text.includes("snow")) {
    return "from-slate-50 to-slate-100";
  }
  if (text.includes("cloud") || text.includes("overcast")) {
    return "from-neutral-50 to-slate-50";
  }
  return "from-amber-50 to-orange-50";
}

function getWeatherEmoji(condition: string | null) {
  const text = condition?.toLowerCase() ?? "";

  if (text.includes("thunder")) return "⛈️";
  if (text.includes("rain") || text.includes("drizzle")) return "🌧️";
  if (text.includes("snow")) return "❄️";
  if (text.includes("fog") || text.includes("mist")) return "🌫️";
  if (text.includes("overcast")) return "☁️";
  if (text.includes("cloud")) return "⛅";
  return "☀️";
}

export default async function EventWeather({
  latitude,
  longitude,
  startAt,
}: Props) {
  const weather = await getWeather(latitude, longitude, startAt).catch(
    () => null,
  );

  if (weather === null) {
    return null;
  }

  if (!weather.isAvailable) {
    return (
      <div className="rounded-xl bg-neutral-50 p-4">
        <p className="font-semibold text-gray-400 text-xs uppercase tracking-widest">
          Weather at start
        </p>
        <p className="mt-3 font-bold text-gray-900 text-lg">
          Forecast not ready
        </p>
        <p className="mt-1 text-gray-500 text-sm">
          Check back closer to the event date
        </p>
      </div>
    );
  }

  return (
    <div
      className={`rounded-xl bg-linear-to-br p-4 ${getWeatherStyle(weather.condition)}`}
    >
      <p className="font-semibold text-gray-400 text-xs uppercase tracking-widest">
        Weather at start
      </p>
      <div className="mt-3 flex items-center gap-3">
        <span className="text-4xl">{getWeatherEmoji(weather.condition)}</span>
        <div>
          <p className="font-bold text-3xl text-gray-900">
            {weather.temperature}°
          </p>
          <p className="text-gray-600">{weather.condition}</p>
        </div>
      </div>
      <div className="mt-3 flex items-baseline gap-3">
        <p className="font-bold text-4xl text-gray-900">
          {weather.temperature}°
        </p>
        <p className="text-gray-600">{weather.condition}</p>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-3">
        {weather.precipitationChance !== null && (
          <div className="rounded-lg bg-white/70 p-3">
            <p className="text-gray-500 text-sm">Rain</p>
            <p className="font-bold text-gray-900 text-lg">
              {weather.precipitationChance}%
            </p>
          </div>
        )}
        {weather.windSpeed !== null && (
          <div className="rounded-lg bg-white/70 p-3">
            <p className="text-gray-500 text-sm">Wind</p>
            <p className="font-bold text-gray-900 text-lg">
              {weather.windSpeed} m/s
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
