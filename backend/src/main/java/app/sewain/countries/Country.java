package app.sewain.countries;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

@Entity
@Table(name = "countries", schema = "public")
@Getter
@Setter
public class Country {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 3)
	private String iso3;

	@Column(name = "numeric_code", length = 3)
	private String numericCode;

	@Column(length = 2)
	private String iso2;

	@Column(length = 255)
	private String phonecode;

	@Column(length = 255)
	private String capital;

	@Column(length = 255)
	private String currency;

	@Column(name = "currency_name", length = 255)
	private String currencyName;

	@Column(name = "currency_symbol", length = 255)
	private String currencySymbol;

	@Column(length = 255)
	private String tld;

	@Column(name = "native", length = 255)
	private String nativeName;

	private Long population;

	private Long gdp;

	@Column(length = 255)
	private String region;

	@Column(name = "region_id")
	private Long regionId;

	@Column(length = 255)
	private String subregion;

	@Column(name = "subregion_id")
	private Long subregionId;

	@Column(length = 255)
	private String nationality;

	@Column(name = "area_sq_km")
	private Double areaSqKm;

	@Column(name = "postal_code_format")
	private String postalCodeFormat;

	@Column(name = "postal_code_regex")
	private String postalCodeRegex;
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private JsonNode timezones;

	@Column(columnDefinition = "text")
	private String translations;

	@Column(precision = 10, scale = 8)
	private java.math.BigDecimal latitude;

	@Column(precision = 11, scale = 8)
	private java.math.BigDecimal longitude;

	@Column(length = 191)
	private String emoji;

	@Column(name = "\"emojiU\"", length = 191)
	private String emojiU;

	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@Column(nullable = false)
	private Short flag;

	@Column(name = "\"wikiDataId\"", length = 255)
	private String wikiDataId;
}