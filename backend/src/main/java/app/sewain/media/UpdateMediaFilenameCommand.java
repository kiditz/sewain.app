package app.sewain.media;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record UpdateMediaFilenameCommand(@NotBlank @Size(max = 255) String filename) {
}
