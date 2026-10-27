package app.sewain.media;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
interface MediaMapper {
    MediaResponse toResponse(Media media);

    void updateFilename(UpdateMediaFilenameCommand command, @MappingTarget Media media);
}
