"""Build required event data with optional venue-setting enrichment."""

import logging
from collections.abc import Callable

logger = logging.getLogger(__name__)


class DbtBuildError(RuntimeError):
    """A dbt process finished with a non-zero exit code."""


def build_with_optional_venue_enrichment(
    run: Callable[[list[str]], None],
    notify: Callable[[str], None],
) -> str:
    """Allow fallback only when the venue-enrichment dbt process fails.

    Required base-model and final-mart failures always propagate.
    """
    run(
        [
            "--exclude",
            "fct_event_attributes+",
            "fct_external_events_enriched",
            "--vars",
            '{"venue_enrichment_available": false}',
        ]
    )

    enrichment_available = True

    try:
        run(
            [
                "--select",
                "fct_event_attributes",
                "--vars",
                '{"venue_enrichment_available": true}',
            ]
        )
    except DbtBuildError:
        enrichment_available = False
        message = (
            "Venue enrichment failed. Continuing with venue_setting=unknown "
            "for all current events. See the dbt_build task log for details."
        )
        logger.warning(message)

        try:
            notify(message)
        except Exception:
            logger.exception("Could not send the venue-enrichment warning")

    run(
        [
            "--select",
            "fct_external_events_enriched",
            "--vars",
            (
                '{"venue_enrichment_available": true}'
                if enrichment_available
                else '{"venue_enrichment_available": false}'
            ),
        ]
    )

    if enrichment_available:
        return "Required models and enriched mart built successfully"

    return "Required models built successfully; venue enrichment used unknown fallback"
