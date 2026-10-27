package app.sewain.catalogs;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "attribute_values",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attribute_values_slug",
                        columnNames = {"attribute_id", "slug"}
                ),
                @UniqueConstraint(
                        name = "uk_attribute_values_value",
                        columnNames = {"attribute_id", "value"}
                ),
                @UniqueConstraint(
                        name = "uk_attribute_values_id_attribute",
                        columnNames = {"id", "attribute_id"}
                )
        }
)
public class AttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_id", nullable = false)
    private Attribute attribute;

    @Column(nullable = false, length = 100)
    private String value;

    @Column(nullable = false, length = 100)
    private String slug;

    @Column(nullable = false)
    private int position = 0;
}