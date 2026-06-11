package vn.edu.fpt.sba.music4.dtos.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record AlbumCreateDTO(
        @NotBlank String albumTitle,
        MultipartFile coverImageKey,
        @NotNull Integer artistId
) {}
