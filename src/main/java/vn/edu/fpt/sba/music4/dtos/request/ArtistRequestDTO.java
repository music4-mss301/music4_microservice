package vn.edu.fpt.sba.music4.dtos.request;

import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record ArtistRequestDTO(
        @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
        String name,

        MultipartFile img,

        MultipartFile cover) {
}
