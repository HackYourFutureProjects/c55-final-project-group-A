// Price can be null on imported events, and zero means free —
// so both need their own check before falling through to a euro amount
export function formatPrice(price: number | null) {
  if (price === null) return "Price unknown";
  if (price === 0) return "Free";
  return `€${price.toFixed(2)}`;
}
