package app.sewain.catalogs;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "collection_rule_values")
@Getter
@Setter
@NoArgsConstructor
class CollectionRuleValue {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "rule_id", nullable = false)
	private CollectionRule rule;

	@Column(name = "value", nullable = false)
	private String value;
}
