package nl.hackyourfuture.project.backend.event.category.service;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.category.dto.CategoryResponse;
import nl.hackyourfuture.project.backend.event.category.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
