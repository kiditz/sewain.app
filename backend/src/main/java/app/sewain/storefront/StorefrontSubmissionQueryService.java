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
    PageResponse<StorefrontSubmission> list(int page,int size) {
        var result=repository.findAll(PageRequest.of(Math.max(0,page),Math.min(100,Math.max(1,size)),Sort.by(Sort.Direction.DESC,"createdAt")));
        return new PageResponse<>(result.getContent(),result.getTotalElements());
    }
}
