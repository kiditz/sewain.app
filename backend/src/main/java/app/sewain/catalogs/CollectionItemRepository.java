package app.sewain.catalogs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Collection;

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
                      and (:status is null or p.status = :status)
                      and (:filterIds = false or p.id in :ids)
                      and (:available = false or exists (select v.id from ProductVariant v where v.product = p and v.active = true))
					order by i.position asc
					""",
			countQuery = """
					select count(i)
					from CollectionItem i
					join i.product p
					where i.collection.id = :collectionId
					  and lower(p.name) like :pattern escape '!'
                      and (:status is null or p.status = :status)
                      and (:filterIds = false or p.id in :ids)
                      and (:available = false or exists (select v.id from ProductVariant v where v.product = p and v.active = true))
					"""
	)
	Page<Product> findProducts(
			@Param("collectionId") UUID collectionId,
			@Param("pattern") String pattern,
            @Param("status") ProductStatus status,
            @Param("filterIds") boolean filterIds,
            @Param("ids") Collection<UUID> ids,
            @Param("available") boolean available,
			Pageable pageable
	);
}
