package app.sewain.merchants;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class MerchantOwnershipTest {
 @Test void rejectsAnotherOwnersMerchantBeforeMappingAnything(){
  var repository=mock(MerchantRepository.class);var mapper=mock(MerchantMapper.class);var merchant=new Merchant();var owner=UUID.randomUUID();var attacker=UUID.randomUUID();var id=UUID.randomUUID();merchant.setUserId(owner);merchant.setName("Original");
  when(repository.findById(id)).thenReturn(Optional.of(merchant));
  var service=new MerchantCommandServiceImpl(repository,mock(MerchantUserRepository.class),mapper,mock(org.springframework.context.ApplicationEventPublisher.class));
  assertThrows(AccessDeniedException.class,()->service.updateMerchant(attacker,id,new MerchantCommand("Changed","changed",null,null,null,false)));
  assertEquals(owner,merchant.getUserId());assertEquals("Original",merchant.getName());verifyNoInteractions(mapper);
 }
 @Test void allowsTheOwnerWithoutTransferringOwnership(){
  var repository=mock(MerchantRepository.class);var mapper=mock(MerchantMapper.class);var merchant=new Merchant();var owner=UUID.randomUUID();var id=UUID.randomUUID();merchant.setUserId(owner);when(repository.findById(id)).thenReturn(Optional.of(merchant));
  var service=new MerchantCommandServiceImpl(repository,mock(MerchantUserRepository.class),mapper,mock(org.springframework.context.ApplicationEventPublisher.class));
  var command=new MerchantCommand("Changed","changed",null,null,null,false);service.updateMerchant(owner,id,command);verify(mapper).update(command,merchant);assertEquals(owner,merchant.getUserId());
 }
}
