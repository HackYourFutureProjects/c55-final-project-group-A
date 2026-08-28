package nl.hackyourfuture.project.backend.event.similarity.model;

import nl.hackyourfuture.project.backend.event.model.EventSummary;

public record SimilarEventCandidate(
        EventSummary event,
        double similarityScore
) {
}