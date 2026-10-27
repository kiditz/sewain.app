package app.sewain.catalogs;

import app.sewain.exception.ConflictException;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class AttributeCommandServiceImpl implements AttributeCommandService {

	private final AttributeRepository attributeRepository;
	private final AttributeMapper mapper;

	@Override
	@Transactional
	public AttributeResponse create(AttributeCommand command) {

		validateSlug(command.slug());
		validateValueSlugs(command);

		var attribute = mapper.toEntity(command);

		addValues(attribute, command);

		return mapper.toResponse(attributeRepository.save(attribute));
	}

	@Override
	@Transactional
	public AttributeResponse update(UUID id, AttributeCommand command) {

		var attribute = findAttribute(id);

		validateSlug(id, command.slug());
		validateValueSlugs(command);

		attribute.setName(command.name());
		attribute.setSlug(command.slug());
		attribute.setDescription(command.description());

		updateValues(attribute, command);

		return mapper.toResponse(attributeRepository.save(attribute));
	}

	@Override
	@Transactional
	public void delete(UUID id) {

		var attribute = findAttribute(id);

//		if (productAttributeValueRepository.existsByAttributeValue_Attribute_Id(id)) {
//
//			throw new ResourceValidationException(ErrorCode.ATTRIBUTE_IN_USE);
//		}

		attributeRepository.delete(attribute);
	}

	private void addValues(Attribute attribute, AttributeCommand command) {

		command.values().forEach(item -> {

			var value = new AttributeValue();

			value.setAttribute(attribute);
			value.setValue(item.value());
			value.setSlug(item.slug());

			attribute.getValues().add(value);
		});
	}

	private void updateValues(
			Attribute attribute,
			AttributeCommand command
	) {
		var existingValues = attribute.getValues();

		var existingById = existingValues.stream()
				.collect(Collectors.toMap(
						AttributeValue::getId,
						value -> value
				));

		var incomingIds = command.values()
				.stream()
				.map(AttributeValueCommand::id)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		/*
		 * Remove values that no longer exist in request.
		 */
		var removed = existingValues.stream()
				.filter(existing -> !incomingIds.contains(existing.getId()))
				.toList();
		existingValues.removeAll(removed);

		/*
		 * Update existing / add new values.
		 */
		for (var item : command.values()) {

			if (item.id() == null) {
				var value = new AttributeValue();

				value.setAttribute(attribute);
				value.setValue(item.value());
				value.setSlug(item.slug());

				existingValues.add(value);

				continue;
			}

			var existing = existingById.get(item.id());

			if (existing == null) {
				throw new ResourceNotFoundException(
						ErrorCode.ATTRIBUTE_VALUE_NOT_FOUND
				);
			}

			existing.setValue(item.value());
			existing.setSlug(item.slug());
		}
	}

	private void validateSlug(String slug) {

		if (attributeRepository.existsBySlug(slug)) {
			throw new ConflictException(ErrorCode.ATTRIBUTE_SLUG_ALREADY_EXISTS);
		}
	}

	private void validateSlug(UUID id, String slug) {

		if (attributeRepository.existsBySlugAndIdNot(slug, id)) {

			throw new ConflictException(ErrorCode.ATTRIBUTE_SLUG_ALREADY_EXISTS);
		}
	}

	private void validateValueSlugs(AttributeCommand command) {

		var slugs = new HashSet<String>();

		for (var value : command.values()) {

			if (!slugs.add(value.slug())) {
				throw new ConflictException(ErrorCode.ATTRIBUTE_VALUE_SLUG_ALREADY_EXISTS);
			}
		}
	}

	private Attribute findAttribute(UUID id) {

		return attributeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ATTRIBUTE_NOT_FOUND));
	}
}