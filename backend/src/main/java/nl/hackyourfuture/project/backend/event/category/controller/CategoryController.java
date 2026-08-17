package nl.hackyourfuture.project.backend.event.category.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.category.dto.CategoryResponse;
import nl.hackyourfuture.project.backend.event.category.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(
        name = "Categories",
        description = "Operations on event categories"
)
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(
            summary = "List event categories",
            description = "Returns all available event categories ordered by name."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Categories returned successfully"
    )

    @ApiResponse(
            responseCode = "500",
            description = "Categories could not be retrieved"
    )
    public List<CategoryResponse> getCategories() {
        return categoryService.getCategories();
    }
}
