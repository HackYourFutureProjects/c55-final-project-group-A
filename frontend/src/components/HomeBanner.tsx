interface HomeBannerProps {
  eventCount: number;
}

export default function HomeBanner({ eventCount }: HomeBannerProps) {
  const today = new Date();
  const formattedDate = today.toLocaleDateString("en-GB", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });

  return (
    <div>
      <div>
        <span>🎵 Music</span>
        <span>🎨 Art</span>
        <span>⚽ Sports</span>
        <span>🎭 Theatre</span>
      </div>

      <h1>Discover events across the Netherlands</h1>
      <p>
        {formattedDate} · {eventCount} events found
      </p>

      {/* TODO: wire up in frontend-event-search */}
      <input
        type="text"
        placeholder="Search events, venues, artists..."
        disabled
      />

      {/* TODO: wire up in frontend-event-sort */}
      <div>
        <label htmlFor="sort">Sort:</label>
        <select id="sort" defaultValue="date">
          <option value="date">Date</option>
          <option value="priceAsc">Price (low to high)</option>
          <option value="priceDesc">Price (high to low)</option>
          <option value="popularity">Popularity</option>
        </select>
      </div>
    </div>
  );
}
