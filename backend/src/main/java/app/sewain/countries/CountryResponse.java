package app.sewain.countries;

import tools.jackson.databind.JsonNode;

public record CountryResponse(
    Long id,
    String name,
    String iso2,
    String iso3,
    String phonecode,
    String currency,
    String currencyName,
    String currencySymbol,
    String emoji,
    JsonNode timezones
) {}