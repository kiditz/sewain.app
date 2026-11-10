package app.sewain.customers;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CustomerStorefrontCommandServiceTest {
	private final CustomerRepository repository = mock(CustomerRepository.class);
	private final CustomerStorefrontCommandService service = new CustomerStorefrontCommandService(repository);

	@Test
	void aNewEmailSignupCreatesOneCustomerAndRecordsItsConsent() {
		var customer = new Customer();
		customer.setId(UUID.randomUUID());
		when(repository.findFirstByEmailIgnoreCaseOrderByCreatedAtAscIdAsc("person@example.com")).thenReturn(Optional.empty(), Optional.of(customer));
		assertEquals(customer.getId(), service.resolve("", " Person@Example.com ", true));
		verify(repository).insertStorefrontContact(any(UUID.class), eq("person@example.com"), eq("person@example.com"), eq("EMAIL_SIGNUP"), eq(true));
	}

	@Test
	void existingProfilesAndDisabledAccountsRemainIntact() {
		var customer = new Customer();
		customer.setId(UUID.randomUUID());
		customer.setFullName("Original name");
		customer.setEnabled(false);
		customer.setPasswordHash("existing-hash");
		customer.setNotes("Merchant notes");
		when(repository.findFirstByEmailIgnoreCaseOrderByCreatedAtAscIdAsc("person@example.com")).thenReturn(Optional.of(customer));
		assertEquals(customer.getId(), service.resolve("Public form name", "PERSON@example.com", false));
		assertEquals("Original name", customer.getFullName());
		assertFalse(customer.isEnabled());
		assertEquals("existing-hash", customer.getPasswordHash());
		assertEquals("Merchant notes", customer.getNotes());
		verify(repository, never()).insertStorefrontContact(any(), anyString(), anyString(), anyString(), anyBoolean());
		verify(repository, never()).subscribeEmail(any());
	}

	@Test
	void signupReusesAnExistingContactAndAConcurrentInsertCanBeResolved() {
		var customer = new Customer();
		customer.setId(UUID.randomUUID());
		when(repository.findFirstByEmailIgnoreCaseOrderByCreatedAtAscIdAsc("person@example.com")).thenReturn(Optional.empty(), Optional.of(customer));
		when(repository.insertStorefrontContact(any(), anyString(), anyString(), anyString(), anyBoolean())).thenReturn(0);
		assertEquals(customer.getId(), service.resolve("Person", "person@example.com", true));
		verify(repository).subscribeEmail(customer.getId());
	}

	@Test
	void normalCustomerUpdatesPreserveTheAcquisitionSourceAndEmailConsent() {
		var customer = new Customer();
		customer.setSource("EMAIL_SIGNUP");
		customer.setEmailSubscribed(true);
		var consent = OffsetDateTime.now();
		customer.setEmailConsentAt(consent);
		Mappers.getMapper(CustomerMapper.class).update(new CustomerCommand("Person", "person@example.com", null, null), customer);
		assertEquals("EMAIL_SIGNUP", customer.getSource());
		assertTrue(customer.isEmailSubscribed());
		assertEquals(consent, customer.getEmailConsentAt());
	}
}
