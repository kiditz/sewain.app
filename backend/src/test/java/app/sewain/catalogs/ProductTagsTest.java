package app.sewain.catalogs;

import app.sewain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductTagsTest {
    private ProductRepository products;
    private TagRepository tags;
    private ProductMapper mapper;
    private ProductCommandService service;

    @BeforeEach
    void setup() {
        products = mock(ProductRepository.class);
        tags = mock(TagRepository.class);
        mapper = mock(ProductMapper.class);
        service = new ProductCommandServiceImpl(products, mock(AttributeValueRepository.class),
                mock(InventoryUnitRepository.class), mapper, mock(ProductVariantMapper.class),
                mock(VariantRentalPlanMapper.class), mock(ProductTypeRepository.class), tags, mock(InventoryUnitMapper.class));
        when(products.saveAndFlush(any(Product.class))).thenAnswer(call -> call.getArgument(0));
    }
    private ProductCommand command(List<UUID> ids) {
        return new ProductCommand("Tent", "tent", "", "", ProductStatus.DRAFT, null, ids, List.of());
    }
    private Tag tag() {
        var tag = new Tag(); tag.setId(UUID.randomUUID()); tag.setName("Camping"); tag.setSlug("camping"); return tag;
    }

    @Test
    void createsProductWithExistingTagsAndDeduplicatesIds() {
        var product = new Product();
        var tag = tag();
        when(mapper.toEntity(any())).thenReturn(product);
        when(tags.findAllById(any())).thenReturn(List.of(tag));
        service.create(command(List.of(tag.getId(), tag.getId())));
        assertEquals(Set.of(tag), product.getTags());
        verify(products).saveAndFlush(product);
    }

    @Test
    void explicitEmptyClearsTagsWhileOmittedFieldPreservesExistingTags() {
        var product = new Product(); product.setId(UUID.randomUUID());
        var tag = tag(); product.getTags().add(tag);
        when(products.findById(product.getId())).thenReturn(Optional.of(product));
        service.update(product.getId(), command(null));
        assertEquals(Set.of(tag), product.getTags());
        verifyNoInteractions(tags);
        when(tags.findAllById(any())).thenReturn(List.of());
        service.update(product.getId(), command(List.of()));
        assertTrue(product.getTags().isEmpty());
    }

    @Test
    void unknownTagRejectsSaveBeforeExistingAssociationsAreChanged() {
        var product = new Product(); product.setId(UUID.randomUUID());
        var existing = tag(); product.getTags().add(existing);
        when(products.findById(product.getId())).thenReturn(Optional.of(product));
        when(tags.findAllById(any())).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class, () -> service.update(product.getId(), command(List.of(UUID.randomUUID()))));
        assertEquals(Set.of(existing), product.getTags());
    }

    @Test
    void productResponseIncludesTagsAndScalarMappingDoesNotOverwriteThem() {
        var actualMapper = Mappers.getMapper(ProductMapper.class);
        ReflectionTestUtils.setField(actualMapper, "productTypeMapper", Mappers.getMapper(ProductTypeMapper.class));
        ReflectionTestUtils.setField(actualMapper, "tagMapper", Mappers.getMapper(TagMapper.class));
        ReflectionTestUtils.setField(actualMapper, "productVariantMapper", Mappers.getMapper(ProductVariantMapper.class));
        var product = new Product(); var tag = tag(); product.getTags().add(tag);
        actualMapper.update(command(List.of()), product);
        assertEquals(Set.of(tag), product.getTags());
        var response = actualMapper.toResponse(product);
        assertEquals(tag.getId(), response.tags().getFirst().id());
        assertEquals("Camping", response.tags().getFirst().name());
    }
}
