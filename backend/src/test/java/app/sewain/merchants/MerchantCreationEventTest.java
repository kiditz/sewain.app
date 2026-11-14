package app.sewain.merchants;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.context.ApplicationEventPublisher;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class MerchantCreationEventTest {
 @Test void createsOwnerMembershipBeforePublishingProvisioningEvent(){var repository=mock(MerchantRepository.class);var membership=mock(MerchantUserRepository.class);var events=mock(ApplicationEventPublisher.class);var service=new MerchantCommandServiceImpl(repository,membership,Mappers.getMapper(MerchantMapper.class),events);var owner=UUID.randomUUID();var merchantId=UUID.randomUUID();when(repository.save(any())).thenAnswer(c->{Merchant m=c.getArgument(0);m.setId(merchantId);assertEquals(owner,m.getUserId());assertTrue(m.isActive());return m;});var response=service.addMerchant(owner,new MerchantCommand("Outdoor","my-outdoor",null,null,null,false));assertEquals(merchantId,response.id());assertEquals("my_outdoor",response.schema());var sequence=inOrder(membership,events);sequence.verify(membership).saveAndFlush(argThat(m->m.getRole()==MerchantRole.OWNER&&m.getId().userId().equals(owner)&&m.getId().merchantId().equals(merchantId)));sequence.verify(events).publishEvent(any(MerchantCreatedEvent.class));}
}
