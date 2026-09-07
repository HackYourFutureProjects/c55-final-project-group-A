export default function TermsOfServicePage() {
  return (
    <main className="mx-auto max-w-3xl px-4 py-12 sm:px-6">
      <h1 className="font-bold text-3xl text-neutral-900 sm:text-4xl">
        Terms of Service
      </h1>
      <p className="mt-2 text-neutral-500 text-sm">
        Last updated: September 2026
      </p>

      <p className="mt-8 text-neutral-700 leading-relaxed">
        By using Loc Events, you agree to the following terms. This service is
        operated from the Netherlands, and these terms are governed by Dutch
        law.
      </p>

      <h2 className="mt-10 font-bold text-neutral-900 text-xl">
        Use of the service
      </h2>
      <p className="mt-4 text-neutral-700 leading-relaxed">
        This platform allows users to discover, save, and attend local events,
        and to create events (for admin users). You agree to use the service
        only for lawful purposes and not to misuse it, including submitting
        false information, spamming, or attempting to disrupt the service.
      </p>

      <h2 className="mt-10 font-bold text-neutral-900 text-xl">Accounts</h2>
      <p className="mt-4 text-neutral-700 leading-relaxed">
        You are responsible for maintaining the security of your account. You
        may sign up using an email and password, or through Google sign-in. You
        must provide accurate information when registering.
      </p>

      <h2 className="mt-10 font-bold text-neutral-900 text-xl">User content</h2>
      <p className="mt-4 text-neutral-700 leading-relaxed">
        Any content you submit (comments, feedback) should be respectful and
        lawful. We reserve the right to remove content that violates these
        terms.
      </p>

      <h2 className="mt-10 font-bold text-neutral-900 text-xl">
        Event information
      </h2>
      <p className="mt-4 text-neutral-700 leading-relaxed">
        Some events shown on the platform come from third-party sources (such as
        Ticketmaster). We do our best to display accurate information, but we
        are not responsible for the accuracy of third-party event details,
        pricing, or availability. Always check with the event organizer for the
        most current information.
      </p>

      <h2 className="mt-10 font-bold text-neutral-900 text-xl">Disclaimer</h2>
      <p className="mt-4 text-neutral-700 leading-relaxed">
        This service is provided &quot;as is,&quot; without warranties of any
        kind. This is a student project built as part of the HackYourFuture
        program in the Netherlands and is not intended for commercial use.
      </p>

      <h2 className="mt-10 font-bold text-neutral-900 text-xl">
        Governing law
      </h2>
      <p className="mt-4 text-neutral-700 leading-relaxed">
        These terms are governed by the laws of the Netherlands, without regard
        to its conflict of law provisions.
      </p>

      <h2 className="mt-10 font-bold text-neutral-900 text-xl">
        Changes to these terms
      </h2>
      <p className="mt-4 text-neutral-700 leading-relaxed">
        We may update these terms from time to time. Continued use of the
        service after changes constitutes acceptance of the updated terms.
      </p>

      <h2 className="mt-10 font-bold text-neutral-900 text-xl">Contact</h2>
      <p className="mt-4 text-neutral-700 leading-relaxed">
        If you have questions about these Terms of Service, please contact us at{" "}
        <a
          href="mailto:loc.event2026@gmail.com"
          className="text-orange-600 hover:underline"
        >
          loc.event2026@gmail.com
        </a>
        .
      </p>
    </main>
  );
}
