package app.sewain.merchants;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MerchantQueryServiceTest {
 @Test void queryReturnsOnlyRepositoryMembershipAndPublicAccessSkipsMembershipLookup(){var repository=mock(MerchantRepository.class);var membership=mock(MerchantUserRepository.class);var service=new MerchantQueryServiceImpl(repository,membership,Mappers.getMapper(MerchantMapper.class));var user=UUID.randomUUID();var merchant=new Merchant();merchant.setId(UUID.randomUUID());when(repository.findByUserId(user)).thenReturn(List.of(merchant));assertEquals(merchant.getId(),service.findByUser(user).getFirst().id());service.validateAccess(user,"public");verifyNoInteractions(membership);assertThrows(AccessDeniedException.class,()->service.validateAccess(user,"other"));when(membership.existsBySchemaAndUserId("shop",user)).thenReturn(true);assertDoesNotThrow(()->service.validateAccess(user,"shop"));}
}
