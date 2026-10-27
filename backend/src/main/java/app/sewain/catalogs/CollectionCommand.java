package app.sewain.catalogs;

import app.sewain.shared.Slug;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

/**
 * Command create / update collection.
 * <p>
 * Field opsional (null = tidak diubah / pakai default) supaya form lama
 * yang hanya mengirim name, slug, parentId, imageUrl tetap bekerja:
 * <ul>
 *   <li>{@code type} null: MANUAL saat create, tidak berubah saat update.</li>
 *   <li>{@code rules} null: rule tidak disentuh. Wajib ada untuk AUTOMATED.</li>
 *   <li>{@code productIds} null: item tidak disentuh. Urutan list = urutan tampil.</li>
 * </ul>
 */
record CollectionCommand(
		@NotBlank String name,
		@NotBlank @Slug String slug,
		UUID parentId,
		String imageUrl,
		String description,
		CollectionType type,
		CollectionMatchCondition matchCondition,
		@Valid List<CollectionRuleCommand> rules,
		List<UUID> productIds
) {
}
