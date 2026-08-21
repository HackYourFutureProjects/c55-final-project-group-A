package nl.hackyourfuture.project.backend.event.category.repository;

import nl.hackyourfuture.project.backend.event.category.model.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CategoryRepository {

    private final JdbcClient jdbcClient;

    private static final RowMapper<Category> CATEGORY_ROW_MAPPER =
            (rs, rowNum) -> new Category(
                    rs.getObject("id", UUID.class),
                    rs.getString("name")
            );


    public List<Category> findAll() {
        String sql = """
                SELECT id, name
                FROM categories
                ORDER BY name
                """;

        return jdbcClient
                .sql(sql)
                .query(CATEGORY_ROW_MAPPER)
                .list();
    }

    public boolean existsAllByIds(Set<UUID> categoryIds) {
        long existingCategoryCount = jdbcClient
                .sql("""
                        SELECT COUNT(*)
                        FROM categories
                        WHERE id IN (:categoryIds)
                        """)
                .param("categoryIds", categoryIds)
                .query(Long.class)
                .single();

        return existingCategoryCount == categoryIds.size();
    }
}
