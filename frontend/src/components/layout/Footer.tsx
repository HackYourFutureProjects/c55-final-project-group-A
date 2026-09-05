export function Footer() {
  return (
    <footer className="bg-orange-50 border-neutral-200 border-t px-8 py-8">
      <p className="mx-auto max-w-7xl text-center text-neutral-500 text-sm">
        Loc · {new Date().getFullYear()} · Built at HackYourFuture
      </p>
    </footer>
  );
}
