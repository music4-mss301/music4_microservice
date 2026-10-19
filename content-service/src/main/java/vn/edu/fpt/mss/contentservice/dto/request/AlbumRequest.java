package vn.edu.fpt.mss.contentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** img là object key từ media-service. artistId chỉ dùng khi ADMIN tạo hộ. */
public record AlbumRequest(@NotBlank(message = "Tên album không được để trống") @Size(max = 100) String name,
                           String img,
                           Integer artistId) {
}
