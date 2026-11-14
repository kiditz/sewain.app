package app.sewain.media;
import app.sewain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MediaMutationTest {
 @Test void renameChangesDisplayFilenameWithoutReplacingStoredObject(){var repository=mock(MediaRepository.class);var storage=mock(MediaStorage.class);var service=new MediaCommandServiceImpl(repository,storage,Mappers.getMapper(MediaMapper.class));var id=UUID.randomUUID();var media=new Media();media.setId(id);media.setObjectKey("media/original.png");when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(media));when(repository.save(media)).thenReturn(media);var result=service.updateFilename(id,new UpdateMediaFilenameCommand("renamed.png"));assertEquals("renamed.png",result.filename());assertEquals("media/original.png",media.getObjectKey());verifyNoInteractions(storage);}
 @Test void deleteRemovesObjectBeforeMetadataAndUnknownIdsAreRejected(){var repository=mock(MediaRepository.class);var storage=mock(MediaStorage.class);var service=new MediaCommandServiceImpl(repository,storage,Mappers.getMapper(MediaMapper.class));var id=UUID.randomUUID();var media=new Media();media.setObjectKey("image.png");when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(media));service.delete(id);var sequence=inOrder(storage,repository);sequence.verify(storage).delete("image.png");sequence.verify(repository).delete(media);var missing=UUID.randomUUID();assertThrows(ResourceNotFoundException.class,()->service.delete(missing));assertThrows(ResourceNotFoundException.class,()->service.updateFilename(missing,new UpdateMediaFilenameCommand("name.png")));}
}
