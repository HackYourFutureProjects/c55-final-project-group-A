import json
from subprocess import TimeoutExpired

import pytest

from src.common.health_metrics_build import build_optional_health_metrics
from src.common.venue_enrichment_build import DbtBuildError


def test_success_builds_only_health_metrics():
    calls = []
    messages = []

    result = build_optional_health_metrics(calls.append, messages.append)

    assert result is True
    assert calls == [
        [
            "--select",
            "agg_event_processing_metrics",
            "--vars",
            '{"venue_enrichment_available": false}',
        ]
    ]
    assert messages == []


@pytest.mark.parametrize(
    "error",
    [
        DbtBuildError("metrics test failed"),
        TimeoutExpired(cmd="dbt", timeout=1800),
    ],
)
def test_metrics_failure_returns_false_and_warns(error):
    messages = []

    def fail(_arguments):
        raise error

    result = build_optional_health_metrics(fail, messages.append)

    assert result is False
    assert len(messages) == 1
    assert "must not be published" in messages[0]


def test_notification_failure_does_not_propagate():
    def fail_build(_arguments):
        raise DbtBuildError("metrics unavailable")

    def fail_notify(_message):
        raise RuntimeError("notification unavailable")

    assert build_optional_health_metrics(fail_build, fail_notify) is False


def test_build_id_is_passed_to_dbt():
    calls = []
    build_id = "00000000-0000-0000-0000-000000000001"

    result = build_optional_health_metrics(
        calls.append,
        lambda message: None,
        build_id=build_id,
    )

    assert result is True
    arguments = calls[0]
    variables = json.loads(arguments[arguments.index("--vars") + 1])
    assert variables["health_metrics_build_id"] == build_id
    assert variables["venue_enrichment_available"] is False
