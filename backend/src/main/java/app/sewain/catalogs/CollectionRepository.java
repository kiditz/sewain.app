package app.sewain.catalogs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CollectionRepository extends JpaRepository<Collection, UUID>, JpaSpecificationExecutor<Collection> {
	Optional<Collection> findBySlug(String slug);

	@Query("""
			select c
			from Collection c
			where c.parent.id = :parentId
			order by c.position asc
			""")
	List<Collection> findSiblings(@Param("parentId") UUID parentId);

	@Query("""
			select c
			from Collection c
			where c.parent is null
			order by c.position asc
			""")
	List<Collection> findRootCategories();

	@Query("""
       select coalesce(max(c.position), -1)
       from Collection c
       where c.parent.id = :parentId
       """)
	int findMaxSortOrderByParentId(@Param("parentId") UUID parentId);

	@Query("""
       select coalesce(max(c.position), -1)
       from Collection c
       where c.parent is null
       """)
	int findMaxSortOrderForRoot();

	@Query(
			value = """
            WITH RECURSIVE collection_tree AS (
                SELECT
                    c.id,
                    c.parent_id,
                    c.position,
                    ARRAY[c.position] AS sort_path
                FROM collections c
                WHERE c.parent_id IS NULL

                UNION ALL

                SELECT
                    c.id,
                    c.parent_id,
                    c.position,
                    ct.sort_path || c.position
                FROM collections c
                JOIN collection_tree ct
                    ON c.parent_id = ct.id
            )
            SELECT c.*
            FROM collections c
            JOIN collection_tree ct
                ON ct.id = c.id
            WHERE (
                :search IS NULL
                OR :search = ''
                OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.slug) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            ORDER BY ct.sort_path
            """,
			countQuery = """
            SELECT COUNT(*)
            FROM collections c
            WHERE (
                :search IS NULL
                OR :search = ''
                OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.slug) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            """,
			nativeQuery = true
	)
	Page<Collection> search(
			@Param("search") String search,
			Pageable pageable
	);


}
