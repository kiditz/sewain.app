package app.sewain.catalogs;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "collections")
@Getter
@Setter
@NoArgsConstructor
class Collection {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String slug;

	@Column(name = "image_url")
	private String imageUrl;

	@Column(columnDefinition = "TEXT")
	private String description;
	@Column(name = "position", nullable = false)
	private Integer position = 0;

	@Enumerated(EnumType.STRING)
	@Column(name = "collection_type", nullable = false, length = 20)
	private CollectionType type = CollectionType.MANUAL;

	@Enumerated(EnumType.STRING)
	@Column(name = "match_condition", nullable = false, length = 10)
	private CollectionMatchCondition matchCondition = CollectionMatchCondition.ALL;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Collection parent;

	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	private List<Collection> children = new ArrayList<>();

	/** Rule untuk collection AUTOMATED. */
	@OneToMany(
			mappedBy = "collection",
			fetch = FetchType.LAZY,
			cascade = CascadeType.ALL,
			orphanRemoval = true
	)
	@OrderBy("position ASC")
	private List<CollectionRule> rules = new ArrayList<>();

	/** Produk pilihan manual untuk collection MANUAL (terurut). */
	@OneToMany(
			mappedBy = "collection",
			fetch = FetchType.LAZY,
			cascade = CascadeType.ALL,
			orphanRemoval = true
	)
	@OrderBy("position ASC")
	private List<CollectionItem> items = new ArrayList<>();

	/**
	 * Jumlah produk manual. Dihitung di SQL yang sama dengan entity
	 * sehingga daftar collection tidak N+1. Untuk AUTOMATED nilainya 0.
	 */
	@Formula("(select count(*) from collection_items ci where ci.collection_id = id)")
	private Long productCount;

}
