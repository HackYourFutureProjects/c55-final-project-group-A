"""Unit tests for the event-attribute LLM model.

These tests cover prompt construction, response validation, batching and rate
limit handling without using a real API key or making network requests.
"""

import email.message
import io
import json
import urllib.error

import pytest
from fct_event_attributes import (
    BATCH_SIZE,
    REQUEST_INTERVAL_SECONDS,
    VENUE_SETTINGS,
    ClassificationError,
    build_prompt,
    classify,
    litellm,
    parse_response,
)


def event(number: int) -> dict[str, str]:
    """Create one event input for a unit test."""

    return {
        "logical_event_id": f"00000000-0000-0000-0000-{number:012d}",
        "title": f"Event {number}",
        "description": f"Description {number}",
        "venue": f"Venue {number}",
        "categories": "Music",
    }


def answer_for(results: list[str]) -> str:
    """Create one response in the shape requested by the prompt."""

    return json.dumps(
        {
            str(index): {
                "venue_setting": venue_setting,
            }
            for index, venue_setting in enumerate(results)
        }
    )


def test_every_event_gets_a_venue_setting():
    events = [event(1), event(2)]
    calls: list[str] = []

    def call(prompt: str) -> str:
        calls.append(prompt)
        return answer_for(
            [
                "indoor",
                "outdoor",
            ]
        )

    assert classify(events, call) == {
        "00000000-0000-0000-0000-000000000001": "indoor",
        "00000000-0000-0000-0000-000000000002": "outdoor",
    }
    assert len(calls) == 1


def test_identical_inputs_are_classified_once_for_separate_logical_events():
    first = event(1)
    second = {
        **event(2),
        "title": first["title"],
        "description": first["description"],
        "venue": first["venue"],
        "categories": first["categories"],
    }
    calls: list[str] = []

    def call(prompt: str) -> str:
        calls.append(prompt)
        return answer_for(["indoor"])

    assert classify([first, second], call) == {
        first["logical_event_id"]: "indoor",
        second["logical_event_id"]: "indoor",
    }
    assert len(calls) == 1
    assert calls[0].count('"title": "Event 1"') == 1


def test_events_are_batched():
    events = [event(index) for index in range(BATCH_SIZE * 2 + 1)]
    calls: list[str] = []
    pauses: list[float] = []

    def call(prompt: str) -> str:
        calls.append(prompt)
        item_count = sum(1 for line in prompt.splitlines() if line.partition(".")[0].isdigit())
        return answer_for(["unknown"] * item_count)

    classify(events, call, pause=pauses.append)

    assert len(calls) == 3
    assert pauses == [
        REQUEST_INTERVAL_SECONDS,
        REQUEST_INTERVAL_SECONDS,
    ]


def test_value_outside_the_contract_becomes_unknown():
    result = parse_response(
        answer_for(["stadium"]),
        [event(1)],
    )

    assert result == {
        "00000000-0000-0000-0000-000000000001": "unknown",
    }


@pytest.mark.parametrize(
    ("description", "model_setting", "expected_setting"),
    [
        ("", "outdoor", "unknown"),
        ("The roof will be closed during the event.", "outdoor", "indoor"),
        ("The roof will be open during the event.", "indoor", "outdoor"),
    ],
)
def test_retractable_roof_requires_event_specific_information(
    description: str,
    model_setting: str,
    expected_setting: str,
):
    arena_event = {
        **event(1),
        "venue": "Johan Cruijff ArenA",
        "description": description,
    }

    assert parse_response(
        answer_for([model_setting]),
        [arena_event],
    ) == {
        arena_event["logical_event_id"]: expected_setting,
    }


def test_missing_answer_becomes_unknown():
    events = [event(1), event(2)]
    content = answer_for(["indoor"])

    assert parse_response(content, events) == {
        "00000000-0000-0000-0000-000000000001": "indoor",
        "00000000-0000-0000-0000-000000000002": "unknown",
    }


def test_json_wrapped_in_prose_is_still_read():
    content = "Here is the result:\n" '```json\n{"0":{"venue_setting":"mixed"}}\n```\n'

    assert parse_response(content, [event(1)]) == {
        "00000000-0000-0000-0000-000000000001": "mixed",
    }


def test_answer_without_json_raises():
    with pytest.raises(ClassificationError, match="no JSON"):
        parse_response("I cannot classify this event.", [event(1)])


def test_prompt_contains_the_contract_and_event_information():
    prompt = build_prompt([event(1)])

    for value in VENUE_SETTINGS:
        assert value in prompt

    assert "Event 1" in prompt
    assert "Description 1" in prompt
    assert "Venue 1" in prompt
    assert "untrusted data" in prompt
    assert "age_restriction" not in prompt


def test_prompt_defines_conservative_classification_rules():
    prompt = build_prompt([event(1)])

    assert "inside an enclosed building" in prompt
    assert "open-air location" in prompt
    assert "both indoor and outdoor areas" in prompt
    assert "Venue names may be used as evidence" in prompt
    assert "retractable roof" in prompt
    assert "roof is closed or open" in prompt
    assert "Otherwise use unknown" in prompt
    assert "event category alone" in prompt


def test_rate_limit_error_is_explained(monkeypatch):
    upstream = b'{"error":{"message":"team rate limit reached"}}'

    def refuse(*_args, **_kwargs):
        raise urllib.error.HTTPError(
            url="",
            code=429,
            msg="",
            hdrs=email.message.Message(),
            fp=io.BytesIO(upstream),
        )

    monkeypatch.setattr("urllib.request.urlopen", refuse)

    with pytest.raises(ClassificationError) as raised:
        litellm("not-a-real-key")("classify these events")

    message = str(raised.value)
    assert "rate limit or daily budget" in message
    assert "team rate limit reached" in message
