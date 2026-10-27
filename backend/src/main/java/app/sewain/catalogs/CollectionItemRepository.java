package app.sewain.catalogs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface CollectionItemRepository extends JpaRepository<CollectionItem, UUID> {

	/**
	 * Produk collection MANUAL sesuai urutan position.
	 *
	 * @param pattern pola LIKE huruf kecil, mis. "%tenda%" ("%" = semua)
	 */
	@Query(
			value = """
					select p
					from CollectionItem i
					join i.product p
					where i.collection.id = :collectionId
					  and lower(p.name) like :pattern escape '!'
					order by i.position asc
					""",
			countQuery = """
					select count(i)
					from CollectionItem i
					join i.product p
					where i.collection.id = :collectionId
					  and lower(p.name) like :pattern escape '!'
					"""
	)
	Page<Product> findProducts(
			@Param("collectionId") UUID collectionId,
			@Param("pattern") String pattern,
			Pageable pageable
	);
}
