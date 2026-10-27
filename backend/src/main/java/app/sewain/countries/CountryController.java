package app.sewain.countries;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
@RequiredArgsConstructor
@Tag(name = "Countries")
class CountryController {

    private final CountryQueryService countryQueryService;

    @GetMapping
    public List<CountryResponse> findAll() {
        return countryQueryService.findAll();
    }
}
