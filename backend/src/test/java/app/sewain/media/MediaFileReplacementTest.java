package app.sewain.media;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mapstruct.factory.Mappers;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MediaFileReplacementTest {
	@TempDir
	Path directory;

	private byte[] transparentPng() throws Exception {
		var image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(1, 1, 0x80abcdef);
		var output = new ByteArrayOutputStream();
		ImageIO.write(image, "png", output);
		return output.toByteArray();
	}

	private MockMultipartFile png(byte[] content) {
		return new MockMultipartFile("file", "processed.png", "image/png", content);
	}

	@Test
	void replacesBytesAtomicallyAndCanRestoreTheOriginal() throws Exception {
		var target = directory.resolve("original.jpg");
		var original = new byte[]{1, 2, 3};
		var processed = transparentPng();
		Files.write(target, original);
		var storage = new LocalMediaStorage(new MediaProperties(directory.toString(), "https://shop.test/"));

		var first = storage.replace(png(processed), "original.jpg");
		assertArrayEquals(processed, Files.readAllBytes(target));
		first.complete(false);
		assertArrayEquals(original, Files.readAllBytes(target));

		var second = storage.replace(png(processed), "original.jpg");
		second.complete(true);
		assertArrayEquals(processed, Files.readAllBytes(target));
		assertEquals(128, ImageIO.read(target.toFile()).getRGB(1, 1) >>> 24);
		try (var files = Files.list(directory)) {
			assertEquals(1, files.count());
		}
	}

	@Test
	void keepsIdentityAndUrlAndRestoresBytesWhenTheMetadataTransactionRollsBack() throws Exception {
		var entity = new Media();
		entity.setId(UUID.randomUUID());
		entity.setFilename("product.jpg");
		entity.setObjectKey("original.jpg");
		entity.setUrl("https://shop.test/original.jpg");
		entity.setContentType("image/jpeg");
		entity.setSize(3L);
		var original = new byte[]{1, 2, 3};
		Files.write(directory.resolve("original.jpg"), original);
		var repository = mock(MediaRepository.class);
		when(repository.findByIdForUpdate(entity.getId())).thenReturn(Optional.of(entity));
		when(repository.saveAndFlush(any(Media.class))).thenAnswer(call -> call.getArgument(0));
		var storage = new LocalMediaStorage(new MediaProperties(directory.toString(), "https://shop.test/"));
		var commands = new MediaCommandServiceImpl(repository, storage, Mappers.getMapper(MediaMapper.class));

		TransactionSynchronizationManager.initSynchronization();
		try {
			var result = commands.replaceFile(entity.getId(), png(transparentPng()));
			assertEquals(entity.getId(), result.id());
			assertEquals("https://shop.test/original.jpg", result.url());
			assertEquals("product.png", result.filename());
			assertEquals("image/png", result.contentType());
			assertTrue(result.backgroundRemoved());
			assertEquals("original.jpg", entity.getObjectKey());
			for (var sync : TransactionSynchronizationManager.getSynchronizations()) {
				sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
			}
			assertArrayEquals(original, Files.readAllBytes(directory.resolve("original.jpg")));
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void rejectsInvalidPngContentBeforeReplacingAnything() {
		var id = UUID.randomUUID();
		var repository = mock(MediaRepository.class);
		when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(new Media()));
		var storage = mock(MediaStorage.class);
		var commands = new MediaCommandServiceImpl(repository, storage, Mappers.getMapper(MediaMapper.class));
		assertThrows(app.sewain.exception.ValidationException.class,
				() -> commands.replaceFile(id, png(new byte[]{1, 2, 3})));
		verifyNoInteractions(storage);
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void servesTransparentPngAtTheOriginalJpegUrlWithTheCorrectContentType() throws Exception {
		var bytes = transparentPng();
		for (var filename : List.of("legacy.jpg", "legacy.svg", "legacy.html")) {
			Files.write(directory.resolve(filename), bytes);
			var handler = new ResourceHttpRequestHandler();
			handler.setServletContext(new MockServletContext());
			handler.setLocations(List.of(new UrlResource(directory.toUri())));
			handler.setResourceResolvers(List.of(new MediaResourceResolver()));
			handler.afterPropertiesSet();
			var request = new MockHttpServletRequest("GET", "/" + filename);
			request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, filename);
			var response = new MockHttpServletResponse();
			handler.handleRequest(request, response);
			assertEquals("image/png", response.getContentType());
			assertArrayEquals(bytes, response.getContentAsByteArray());
		}
	}

    @Test
    void stillRejectsHtmlAndInvalidSvgWhenContentIsNotPng() throws Exception {
        for (var filename : List.of("unsafe.html", "invalid.svg")) {
            Files.writeString(directory.resolve(filename), "<html><script>alert(1)</script></html>");
            var handler = new ResourceHttpRequestHandler();
            handler.setServletContext(new MockServletContext());
            handler.setLocations(List.of(new UrlResource(directory.toUri())));
            handler.setResourceResolvers(List.of(new MediaResourceResolver()));
            handler.afterPropertiesSet();
            var request = new MockHttpServletRequest("GET", "/" + filename);
            request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, filename);
            assertThrows(org.springframework.web.servlet.resource.NoResourceFoundException.class,
                    () -> handler.handleRequest(request, new MockHttpServletResponse()));
        }
    }

	@Test
	void exposesFileDownloadAndMultipartReplacementThroughSeparateQueryAndCommandMethods() throws Exception {
		var id = UUID.randomUUID();
		var bytes = transparentPng();
		var commands = mock(MediaCommandService.class);
		var queries = mock(MediaQueryService.class);
		when(queries.findFile(id)).thenReturn(new MediaFileResponse(new ByteArrayResource(bytes), "image/png", bytes.length));
		when(commands.replaceFile(eq(id), any())).thenReturn(new MediaResponse(id, "product.png", "image/png", (long) bytes.length, "https://shop.test/original.jpg", null, true));
		var mvc = MockMvcBuilders.standaloneSetup(new MediaController(commands, queries)).build();
		mvc.perform(get("/api/media/{id}/file", id))
				.andExpect(status().isOk()).andExpect(content().contentType("image/png"))
				.andExpect(header().string("Cache-Control", "no-store")).andExpect(content().bytes(bytes));
		mvc.perform(multipart(HttpMethod.PUT, "/api/media/{id}/file", id).file(png(bytes)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.url").value("https://shop.test/original.jpg"));
	}

	@Test
	void skipsAlreadyProcessedFilesWithoutTouchingStorage() {
		var id = UUID.randomUUID();
		var entity = new Media();
		entity.setId(id);
		entity.setBackgroundRemoved(true);
		var repository = mock(MediaRepository.class);
		when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(entity));
		var storage = mock(MediaStorage.class);
		var commands = new MediaCommandServiceImpl(repository, storage, Mappers.getMapper(MediaMapper.class));
		assertTrue(commands.replaceFile(id, png(new byte[]{1, 2, 3})).backgroundRemoved());
		verifyNoInteractions(storage);
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void tracksProcessedUploadsIndependentlyOfTheirFilename() throws Exception {
		var repository = mock(MediaRepository.class);
		when(repository.saveAll(any())).thenAnswer(call -> call.getArgument(0));
		var storage = mock(MediaStorage.class);
		when(storage.store(any(), any())).thenReturn(new StoredMedia("key", "https://shop.test/image.png"));
		var commands = new MediaCommandServiceImpl(repository, storage, Mappers.getMapper(MediaMapper.class));
		var file = png(transparentPng());
		var result = commands.upload(List.of(file, file), List.of(1));
		assertFalse(result.get(0).backgroundRemoved());
		assertTrue(result.get(1).backgroundRemoved());
		assertEquals(result.get(0).filename(), result.get(1).filename());
	}
}
