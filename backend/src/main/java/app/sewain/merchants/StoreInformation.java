package app.sewain.merchants;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
class StoreInformation {

	@Column(name = "description", columnDefinition = "text")
	private String description;

	@Column(name = "logo_url")
	private String logoUrl;

	@Column(name = "favicon_url")
	private String faviconUrl;

	@Column(name = "contact_email")
	private String contactEmail;

	@Column(name = "contact_phone")
	private String contactPhone;
}