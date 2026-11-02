package vn.edu.fpt.mss.contentservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Phát hành nhiều bài một lần: file đã upload trước qua /upload-temp, ở đây chỉ gửi key. */
public record AlbumBulkRequest(Integer albumId,
                               String coverImageKey,
                               @NotEmpty(message = "Phải có ít nhất 1 bài hát") @Size(max = 50) @Valid
                               List<TrackBulkItem> tracks) {
}
