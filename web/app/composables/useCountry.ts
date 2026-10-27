export interface CountryTimezone {
  tzName: string;
  zoneName: string;
  gmtOffset: number;
  abbreviation: string;
  gmtOffsetName: string;
}

export interface Country {
  id: number;
  name: string;
  iso2: string;
  iso3: string;
  phonecode: string;
  currency: string;
  currencyName: string;
  currencySymbol: string;
  emoji: string;
  timezones: CountryTimezone[];
}

export function useCountry() {
  const api = useApi();

  async function findCountries() {
    return await api<Country[]>("/countries");
  }

  return {
    findCountries,
  };
}
