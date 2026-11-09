package app.sewain.merchants;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
class ProvisioningSocketController {
    private final MerchantProvisioningService provisioning;
    record Request(UUID merchantId) {}
    record Failure(String error) {}

    @MessageMapping("/provisioning/status")
    @SendToUser(value = "/queue/provisioning", broadcast = false)
    MerchantProvisioningService.Status status(Request request, Principal principal) {
        return provisioning.snapshot(request.merchantId(), UUID.fromString(principal.getName()));
    }
    @MessageMapping("/provisioning/retry")
    @SendToUser(value = "/queue/provisioning", broadcast = false)
    MerchantProvisioningService.Status retry(Request request, Principal principal) {
        return provisioning.retry(request.merchantId(), UUID.fromString(principal.getName()));
    }
    @MessageExceptionHandler
    @SendToUser(value = "/queue/provisioning", broadcast = false)
    Failure failed(Exception error) { return new Failure("provisioning.requestFailed"); }
}
