package app.sewain.catalogs;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
class ProductImportSocketController {
    private final ProductImportService imports;
    record Request(UUID id) {}
    record Failure(String error) {}
    @MessageMapping("/product-imports/status")
    @SendToUser(value="/queue/product-imports",broadcast=false)
    ProductImportService.Status status(Request request,Principal principal) {
        return imports.snapshot(request.id(),UUID.fromString(principal.getName()));
    }
    @MessageExceptionHandler
    @SendToUser(value="/queue/product-imports",broadcast=false)
    Failure failed(Exception error) { return new Failure("Status import tidak dapat diakses"); }
}
