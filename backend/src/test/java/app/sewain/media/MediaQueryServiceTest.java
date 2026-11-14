package app.sewain.media;
import app.sewain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class MediaQueryServiceTest {
 final MediaRepository repository=mock(MediaRepository.class);final MediaStorage storage=mock(MediaStorage.class);
 final MediaQueryServiceImpl service=new MediaQueryServiceImpl(repository,Mappers.getMapper(MediaMapper.class),storage);
 @Test void fileReturnsStoredResourceAndRejectsMissingOrUnreadableObject(){var id=UUID.randomUUID();assertThrows(ResourceNotFoundException.class,()->service.findFile(id));var media=new Media();media.setObjectKey("image.png");media.setSize(2L);media.setContentType("image/png");when(repository.findById(id)).thenReturn(Optional.of(media));when(storage.read("image.png")).thenReturn(new ByteArrayResource(new byte[]{1,2}));assertNotNull(service.findFile(id));var unreadable=mock(Resource.class);when(storage.read("image.png")).thenReturn(unreadable);assertThrows(ResourceNotFoundException.class,()->service.findFile(id));}
 @Test void listUsesDefaultPaginationAndRealResponseMapping(){var media=new Media();media.setId(UUID.randomUUID());when(repository.findAll(any(Specification.class),any(Pageable.class))).thenReturn(new PageImpl<>(List.of(media)));assertEquals(media.getId(),service.findAll(null).items().getFirst().id());}
}
