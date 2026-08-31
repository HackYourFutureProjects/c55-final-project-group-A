type Props = {
  title?: string;
  hint?: string;
};

export default function EmptyState({
  title = "No events found",
  hint = "Try adjusting your search or filters",
}: Props) {
  return (
    <div className="py-16 text-center">
      <p className="font-semibold text-gray-900 text-lg">{title}</p>
      <p className="mt-1 text-gray-500">{hint}</p>
    </div>
  );
}
