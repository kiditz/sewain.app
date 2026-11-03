package app.sewain.storefront;
import java.time.OffsetDateTime;
import java.util.UUID;
record StorefrontSubmissionResponse(UUID id,UUID customerId,String kind,String name,String email,String message,boolean consent,OffsetDateTime createdAt) {
    static StorefrontSubmissionResponse from(StorefrontSubmission submission) {
        return new StorefrontSubmissionResponse(submission.getId(),submission.getCustomerId(),submission.getKind(),submission.getName(),submission.getEmail(),submission.getMessage(),submission.isConsent(),submission.getCreatedAt());
    }
}
