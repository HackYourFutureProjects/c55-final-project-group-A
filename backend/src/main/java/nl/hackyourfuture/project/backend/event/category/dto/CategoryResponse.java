package nl.hackyourfuture.project.backend.event.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import nl.hackyourfuture.project.backend.event.category.model.Category;

import java.util.UUID;


@Schema(description = "An event category")
public record CategoryResponse(

        @Schema(
                description = "Unique category identifier",
                example = "39e87f29-9a69-4c22-bb67-7e9edced2f5e",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID id,

        @Schema(
                description = "Category display name",
                example = "Music",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String name
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.id(),
                category.name()
        );
    }
}