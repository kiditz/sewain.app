package app.sewain.catalogs;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/products/imports")
@RequiredArgsConstructor
class ProductImportController {
    private final ProductImportService imports;
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    ProductImportService.Status upload(@RequestParam MultipartFile file, JwtAuthenticationToken auth) throws IOException {
        return imports.start(file,user(auth));
    }
    @GetMapping("/latest")
    ProductImportService.Status latest(JwtAuthenticationToken auth) { return imports.latest(user(auth)); }
    @GetMapping("/{id}")
    ProductImportService.Status status(@PathVariable UUID id,JwtAuthenticationToken auth) { return imports.snapshot(id,user(auth)); }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    org.springframework.http.ResponseEntity<java.util.Map<String,String>> invalid(org.springframework.web.server.ResponseStatusException error) {
        return org.springframework.http.ResponseEntity.status(error.getStatusCode()).body(java.util.Map.of("message", java.util.Objects.requireNonNullElse(error.getReason(),"Import gagal")));
    }
    private UUID user(JwtAuthenticationToken auth) { return UUID.fromString(auth.getToken().getClaimAsString("user")); }
}
