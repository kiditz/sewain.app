package app.sewain.catalogs;
import app.sewain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class ProductLookupsTest {
 @Test void productLookupByIdOrSlugReturnsMappedProductAndUnknownIsNotFound(){var repository=mock(ProductRepository.class);var mapper=mock(ProductMapper.class);var types=mock(ProductTypeMapper.class);var service=new ProductQueryServiceImpl(repository,mapper,types);var product=new Product();product.setId(UUID.randomUUID());var response=mock(ProductResponse.class);when(repository.findById(product.getId())).thenReturn(Optional.of(product));when(repository.findBySlug("tent")).thenReturn(Optional.of(product));when(mapper.toResponse(product)).thenReturn(response);assertSame(response,service.findById(product.getId()));assertSame(response,service.findBySlug("tent"));assertThrows(ResourceNotFoundException.class,()->service.findById(UUID.randomUUID()));assertThrows(ResourceNotFoundException.class,()->service.findBySlug("missing"));service.findTypes();verify(repository).findTypesByStatus(ProductStatus.ACTIVE);}
 @Test void typeSearchIsBoundedAndDetailUsesRealMapping(){var repository=mock(ProductTypeRepository.class);var service=new ProductTypeQueryServiceImpl(repository,Mappers.getMapper(ProductTypeMapper.class));var type=new ProductType();type.setId(UUID.randomUUID());type.setName("Camping");when(repository.findAll(any(Specification.class),any(Pageable.class))).thenReturn(new PageImpl<>(List.of(type)));when(repository.findById(type.getId())).thenReturn(Optional.of(type));assertEquals("Camping",service.find("camp").getFirst().name());assertEquals(type.getId(),service.findById(type.getId()).id());assertThrows(ResourceNotFoundException.class,()->service.findById(UUID.randomUUID()));var page=org.mockito.ArgumentCaptor.forClass(Pageable.class);verify(repository).findAll(any(Specification.class),page.capture());assertEquals(50,page.getValue().getPageSize());}
}
