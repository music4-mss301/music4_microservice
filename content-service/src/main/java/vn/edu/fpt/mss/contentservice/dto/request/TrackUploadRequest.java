package vn.edu.fpt.mss.contentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Form-data đi kèm audioFile + coverImage. Giữ tên field như FE cũ: title, artistId, albumId, categoryIds, artistIds.
 * artistId chỉ ADMIN được chỉ định; ARTIST luôn là nghệ sĩ chính của bài mình upload.
 */
public record TrackUploadRequest(@NotBlank(message = "Tên bài hát không được để trống") @Size(max = 100) String title,
                                 Integer artistId,
                                 Integer albumId,
                                 List<Integer> categoryIds,
                                 List<Integer> artistIds) {
}
