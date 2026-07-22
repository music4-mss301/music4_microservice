package vn.edu.fpt.sba.music4.dtos.response;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AlbumBulkJsonDTO {
    private Integer albumId;
    private String coverImageKey; // Nhận lại s3Key của ảnh bìa từ Bước 1
    @NotEmpty(message = "Danh sách bài hát không được rỗng!")
    @Valid
    private List<TrackJsonMetadataDTO> tracks; // Danh sách các bài hát bên trên
}