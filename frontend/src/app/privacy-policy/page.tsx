export default function PrivacyPolicyPage() {
  return (
    <main className="mx-auto max-w-3xl px-4 py-12 sm:px-6">
      <h1 className="font-bold text-3xl text-neutral-900 sm:text-4xl">
        Privacy Policy
      </h1>
      <p className="mt-2 text-neutral-500 text-sm">
        Last updated: September 2026
      </p>

      <p className="mt-8 text-neutral-700 leading-relaxed">
        This Privacy Policy explains what information Loc Events collects when
        you use our service and how we use it, in accordance with the EU General
        Data Protection Regulation (GDPR).
      </p>

      <h2 className="mt-10 font-bold text-neutral-900 text-xl">
        Information we collect
      </h2>
      <ul className="mt-4 space-y-3 text-neutral-700 leading-relaxed">
        <li>
          <strong className="font-semibold">Account information:</strong> When
          you register, we collect your name and email address. If you sign in
          with Google, we receive your name and email address from Google.
        </li>
        <li>
          <strong className="font-semibold">Location data:</strong> When
          creating or interacting with events, we may collect address and
          location information (such as city and coordinates) to display events
          on a map and enable location-based search.
        </li>
        <li>
          <strong className="font-semibold">Usage data:</strong> We use cookies
          to keep you signed in and maintain your session while using the site.
        </li>
        <li>
          <strong className="font-semibold">Feedback:</strong> If you submit
          feedback through our feedback form, we store the content you provide,
          along with an optional name and email if you choose to share them.
        </li>
      </ul>

      <h2 className="mt-10 font-bold text-neutral-900 text-xl">
        Legal basis for processing
      </h2>
      <p className="mt-4 text-neutral-700 leading-relaxed">
        We process your personal data based on your consent (when you register
        or sign in) and our legitimate interest in providing and improving the
        service.
      </p>

      <h2 className="mt-10 font-bold text-neutral-900 text-xl">
        How we use your information
      </h2>
      <p className="mt-4 text-neutral-700 leading-relaxed">
        We use the information we collect to:
      </p>
      <ul className="mt-3 list-disc space-y-2 pl-6 text-neutral-700 leading-relaxed">
        <li>Create and manage your account</li>
        <li>Show you relevant events</li>
        <li>
          Let you save events, mark yourself as attending, and leave comments
        </li>
        <li>Respond to feedback, if you&apos;ve asked to be contacted</li>
        <li>Keep you signed in between visits</li>
      </ul>

      <h2 className="mt-10 font-bold text-neutral-900 text-xl">
        Third-party services
      </h2>
      <p className="mt-4 text-neutral-700 leading-relaxed">
        We use the following third-party services to provide our features:
      </p>
      <ul className="mt-3 list-disc space-y-2 pl-6 text-neutral-700 leading-relaxed">
        <li>Google (for sign-in, if you choose this option)</li>
        <li>Open-Meteo (for weather forecasts shown on event pages)</li>
        <li>OpenStreetMap/Nominatim (for address search and mapping)</li>
        <li>
          Google Gemini (to power the AI assistant answering questions about
          events)
        </li>
        <li>
          Ticketmaster (as a source of some event listings shown on our
          platform)
        </li>
      </ul>
      <p className="mt-4 text-neutral-700 leading-relaxed">
        We do not sell your personal information to third parties.
      </p>

      <h2 className="mt-10 font-bold text-neutral-900 text-xl">
        Your rights under GDPR
      </h2>
      <p className="mt-4 text-neutral-700 leading-relaxed">
        As a user based in the Netherlands or elsewhere in the EU, you have the
        right to access, correct, or delete your personal data, and to withdraw
        your consent at any time. You can request account deletion directly, or
        contact us using the details below.
      </p>

      <h2 className="mt-10 font-bold text-neutral-900 text-xl">
        Data retention
      </h2>
      <p className="mt-4 text-neutral-700 leading-relaxed">
        We retain your account information for as long as your account is
        active. You can request deletion of your account and associated data at
        any time.
      </p>

      <h2 className="mt-10 font-bold text-neutral-900 text-xl">Contact</h2>
      <p className="mt-4 text-neutral-700 leading-relaxed">
        If you have questions about this Privacy Policy, please contact us at{" "}
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
