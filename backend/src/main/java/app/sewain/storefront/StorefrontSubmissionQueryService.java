package app.sewain.storefront;
import app.sewain.shared.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class StorefrontSubmissionQueryService {
    private final StorefrontSubmissionRepository repository;
    PageResponse<StorefrontSubmissionResponse> list(int page,int size,java.util.UUID customerId) {
        var pageable=PageRequest.of(Math.max(0,page),Math.min(100,Math.max(1,size)),Sort.by(Sort.Direction.DESC,"createdAt").and(Sort.by(Sort.Direction.DESC,"id")));
        var result=customerId == null ? repository.findAll(pageable) : repository.findByCustomerId(customerId,pageable);
        return new PageResponse<>(result.getContent().stream().map(StorefrontSubmissionResponse::from).toList(),result.getTotalElements());
    }
}
