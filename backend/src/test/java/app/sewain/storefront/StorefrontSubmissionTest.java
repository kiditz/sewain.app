package app.sewain.storefront;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StorefrontSubmissionTest {
    private final StorefrontSubmissionRepository repository=mock(StorefrontSubmissionRepository.class);
    private final app.sewain.customers.CustomerStorefrontCommandService customers=mock(app.sewain.customers.CustomerStorefrontCommandService.class);
    private final StorefrontSubmissionCommandService commands=new StorefrontSubmissionCommandService(repository,customers);
    private final StorefrontSubmissionQueryService queries=new StorefrontSubmissionQueryService(repository);
    @Test void contactNormalizesDataAndRetainsTheRetryIdentifier() {
        var id=UUID.randomUUID();
        var customerId=UUID.randomUUID();
        when(repository.insert(any(),anyString(),anyString(),anyString(),anyString(),anyBoolean())).thenReturn(1);
        when(customers.resolve("Person","person@example.com",false)).thenReturn(customerId);
        commands.submit(new StorefrontSubmissionCommand(id,"CONTACT","  Person  "," Person@Example.com "," Hello ",false,""));
        verify(repository).insert(id,"CONTACT","Person","person@example.com","Hello",false);
        verify(repository).assignCustomer(id,customerId);
    }
    @Test void signupRequiresConsentAndContactRequiresANameAndMessage() {
        var id=UUID.randomUUID();
        assertThrows(ResponseStatusException.class,()->commands.submit(new StorefrontSubmissionCommand(id,"EMAIL_SIGNUP","","a@example.com","",false,"")));
        assertThrows(ResponseStatusException.class,()->commands.submit(new StorefrontSubmissionCommand(id,"CONTACT","","a@example.com","",false,"")));
        verifyNoInteractions(repository);
        commands.submit(new StorefrontSubmissionCommand(id,"EMAIL_SIGNUP","","a@example.com","",true,""));
        verify(repository).insert(id,"EMAIL_SIGNUP","","a@example.com","",true);
    }
    @Test void honeypotSubmissionsDoNotWriteAnyData() {
        commands.submit(new StorefrontSubmissionCommand(UUID.randomUUID(),"CONTACT","Person","a@example.com","Hello",false,"spam"));
        verifyNoInteractions(repository,customers);
    }
    @Test void controllerRejectsInvalidEmailAndAcceptsValidContact() throws Exception {
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new StorefrontSubmissionController(commands,queries)).setControllerAdvice(new StorefrontExceptionHandler()).build();
        mvc.perform(post("/api/storefront/submissions").contentType("application/json").content("""
          {"requestId":"%s","kind":"CONTACT","name":"Person","email":"invalid","message":"Hello"}
          """.formatted(UUID.randomUUID()))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/storefront/submissions").contentType("application/json").content("{\"requestId\":\"invalid-uuid\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/storefront/submissions").contentType("application/json").content("""
          {"requestId":"%s","kind":"CONTACT","name":"Person","email":"a@example.com","message":"Hello"}
          """.formatted(UUID.randomUUID()))).andExpect(result -> assertEquals(202,result.getResponse().getStatus(),()->String.valueOf(result.getResolvedException())));
    }
    @Test void inboxClampsPaginationInsteadOfLoadingAllSubmissions() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        assertEquals(0,queries.list(-1,10000,null).totalItems());
        var page=org.mockito.ArgumentCaptor.forClass(Pageable.class);verify(repository).findAll(page.capture());
        assertEquals(0,page.getValue().getPageNumber());assertEquals(100,page.getValue().getPageSize());
    }

    @Test void retriedRequestsDoNotCreateOrModifyAnotherCustomer() {
        var id=UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);
        commands.submit(new StorefrontSubmissionCommand(id,"EMAIL_SIGNUP","","a@example.com","",true,""));
        verifyNoInteractions(customers);
    }
    @Test void customerHistoryUsesTheCustomerFilterAndBoundedPagination() {
        var customerId=UUID.randomUUID();
        when(repository.findByCustomerId(eq(customerId),any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        queries.list(0,10,customerId);
        verify(repository).findByCustomerId(eq(customerId),argThat(page -> page.getPageSize()==10));
        verify(repository,never()).findAll(any(Pageable.class));
    }
}
