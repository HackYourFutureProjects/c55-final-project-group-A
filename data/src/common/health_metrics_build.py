"""Build optional Health Page metrics without blocking event publication."""

import json
import logging
from collections.abc import Callable
from subprocess import TimeoutExpired
from uuid import UUID

from src.common.venue_enrichment_build import DbtBuildError

logger = logging.getLogger(__name__)


def build_optional_health_metrics(
    run: Callable[[list[str]], None],
    notify: Callable[[str], None],
    *,
    build_id: str | None = None,
) -> bool:
    """Return whether the metrics model and its tests completed successfully."""
    variables = '{"venue_enrichment_available": false}'
    if build_id is not None:
        variables = json.dumps(
            {
                "venue_enrichment_available": False,
                "health_metrics_build_id": str(UUID(build_id)),
            }
        )

    try:
        run(
            [
                "--select",
                "agg_event_processing_metrics",
                "--vars",
                variables,
            ]
        )
    except (DbtBuildError, TimeoutExpired):
        message = (
            "Health Page processing metrics could not be built or validated. "
            "Event publication may continue, but processing metrics from this "
            "attempt must not be published."
        )
        logger.exception(message)

        try:
            notify(message)
        except Exception:
            logger.exception("Could not send the Health Page metrics warning")

        return False

    return True
