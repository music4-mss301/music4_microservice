package vn.edu.fpt.sba.music4.dtos.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AlbumBulkJsonDTO {
    private Integer albumId;
    private String coverImageKey; // Nhận lại s3Key của ảnh bìa từ Bước 1
    private List<TrackJsonMetadataDTO> tracks; // Danh sách các bài hát bên trên
}