"""Test venue-enrichment orchestration without subprocesses or credentials."""

import json
import subprocess

import pytest

from src.common.venue_enrichment_build import (
    DbtBuildError,
    build_with_optional_venue_enrichment,
)


def enrichment_enabled(arguments: list[str]) -> bool:
    variables = json.loads(arguments[arguments.index("--vars") + 1])
    return variables["venue_enrichment_available"]


def test_success_builds_base_then_attributes_then_final_mart():
    calls: list[list[str]] = []
    notifications: list[str] = []

    result = build_with_optional_venue_enrichment(
        calls.append,
        notifications.append,
    )

    assert len(calls) == 3
    assert calls[0][:4] == [
        "--exclude",
        "fct_event_attributes+",
        "fct_external_events_enriched",
        "agg_event_processing_metrics",
    ]
    assert calls[1][:2] == ["--select", "fct_event_attributes"]
    assert calls[2][:2] == ["--select", "fct_external_events_enriched"]
    assert [enrichment_enabled(call) for call in calls] == [False, True, True]
    assert notifications == []
    assert "unknown fallback" not in result


def test_enrichment_failure_builds_final_mart_with_unknown():
    calls: list[list[str]] = []
    notifications: list[str] = []

    def run(arguments: list[str]) -> None:
        calls.append(arguments)
        if len(calls) == 2:
            raise DbtBuildError("Python job could not start")

    result = build_with_optional_venue_enrichment(run, notifications.append)

    assert len(calls) == 3
    assert calls[2][:2] == ["--select", "fct_external_events_enriched"]
    assert enrichment_enabled(calls[2]) is False
    assert len(notifications) == 1
    assert "venue_setting=unknown" in notifications[0]
    assert "unknown fallback" in result


def test_base_failure_stops_before_enrichment():
    calls: list[list[str]] = []
    notifications: list[str] = []

    def run(arguments: list[str]) -> None:
        calls.append(arguments)
        raise DbtBuildError("Base data failed validation")

    with pytest.raises(DbtBuildError, match="Base data"):
        build_with_optional_venue_enrichment(run, notifications.append)

    assert len(calls) == 1
    assert notifications == []


@pytest.mark.parametrize("enrichment_fails", [False, True])
def test_final_mart_failure_always_propagates(enrichment_fails: bool):
    calls: list[list[str]] = []

    def run(arguments: list[str]) -> None:
        calls.append(arguments)
        if len(calls) == 2 and enrichment_fails:
            raise DbtBuildError("Enrichment failed")
        if len(calls) == 3:
            raise DbtBuildError("Final mart failed validation")

    with pytest.raises(DbtBuildError, match="Final mart"):
        build_with_optional_venue_enrichment(run, lambda message: None)

    assert len(calls) == 3
    assert enrichment_enabled(calls[2]) is not enrichment_fails


def test_notification_failure_does_not_stop_fallback(caplog):
    calls: list[list[str]] = []

    def run(arguments: list[str]) -> None:
        calls.append(arguments)
        if len(calls) == 2:
            raise DbtBuildError("Enrichment unavailable")

    def notify(message: str) -> None:
        raise RuntimeError("Slack unavailable")

    result = build_with_optional_venue_enrichment(run, notify)

    assert len(calls) == 3
    assert enrichment_enabled(calls[2]) is False
    assert "unknown fallback" in result
    assert "Could not send the venue-enrichment warning" in caplog.text


def test_unexpected_runner_error_is_not_silenced():
    calls: list[list[str]] = []

    def run(arguments: list[str]) -> None:
        calls.append(arguments)
        if len(calls) == 2:
            raise ValueError("Invalid runner configuration")

    with pytest.raises(ValueError, match="Invalid runner"):
        build_with_optional_venue_enrichment(run, lambda message: None)

    assert len(calls) == 2


def test_timeout_stops_without_starting_final_mart():
    calls: list[list[str]] = []

    def run(arguments: list[str]) -> None:
        calls.append(arguments)
        if len(calls) == 2:
            raise subprocess.TimeoutExpired(cmd="dbt", timeout=1800)

    with pytest.raises(subprocess.TimeoutExpired):
        build_with_optional_venue_enrichment(run, lambda message: None)

    assert len(calls) == 2
