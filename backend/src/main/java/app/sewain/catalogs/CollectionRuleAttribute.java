package app.sewain.catalogs;

/**
 * Atribut produk yang bisa dipakai di rule collection otomatis.
 */
enum CollectionRuleAttribute {
	/** Nama produk. */
	TITLE,
	/** Nama product type. */
	TYPE,
	/** Salah satu tag produk. */
	TAG,
	/** Nama salah satu varian produk. */
	VARIANT_TITLE,
	/** Salah satu nilai atribut varian (mis. "Merah", "60L"). */
	ATTRIBUTE_VALUE
}
