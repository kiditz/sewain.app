package app.sewain.countries;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface CountryRepository extends JpaRepository<Country, Long> {

    List<Country> findAllByOrderByNameAsc();
}
