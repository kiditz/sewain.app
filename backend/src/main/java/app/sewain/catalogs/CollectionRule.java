package app.sewain.catalogs;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "collection_rules")
@Getter
@Setter
@NoArgsConstructor
class CollectionRule {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "collection_id", nullable = false)
	private Collection collection;

	@Enumerated(EnumType.STRING)
	@Column(name = "attribute", nullable = false, length = 100)
	private CollectionRuleAttribute attribute;

	@Enumerated(EnumType.STRING)
	@Column(name = "operator", nullable = false, length = 20)
	private CollectionRuleOperator operator;

	@Column(name = "position", nullable = false)
	private Integer position = 0;

	@OneToMany(
			mappedBy = "rule",
			fetch = FetchType.LAZY,
			cascade = CascadeType.ALL,
			orphanRemoval = true
	)
	@OrderBy("value ASC")
	private List<CollectionRuleValue> values = new ArrayList<>();

	void addValue(String value) {
		var ruleValue = new CollectionRuleValue();
		ruleValue.setRule(this);
		ruleValue.setValue(value);
		values.add(ruleValue);
	}
}
