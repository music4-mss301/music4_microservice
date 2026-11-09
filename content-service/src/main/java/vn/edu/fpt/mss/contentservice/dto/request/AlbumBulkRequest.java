package vn.edu.fpt.mss.contentservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Phát hành nhiều bài một lần: file đã upload trước qua /upload-temp, ở đây chỉ gửi key. */
public record AlbumBulkRequest(
        @NotNull(message = "Thiếu mã album")
        Integer albumId,

        @NotBlank(message = "Album phải có ảnh bìa")
        String coverImageKey,

        @NotEmpty(message = "Phải có ít nhất 1 bài hát")
        @Size(max = 50, message = "Album chỉ chứa tối đa 50 bài hát")
        @Valid
        List<TrackBulkItem> tracks) {
}
