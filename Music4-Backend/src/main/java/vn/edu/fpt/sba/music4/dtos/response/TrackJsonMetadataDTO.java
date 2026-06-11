package vn.edu.fpt.sba.music4.dtos.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TrackJsonMetadataDTO {
    private String title;
    private String audioFileKey; // Nhận lại s3Key từ Bước 1
    private Integer duration;    // Nhận lại duration từ Bước 1
    private Integer artistId;    // Ca sĩ chính bài này
    private List<Integer> artistIds; // Ca sĩ feat riêng của bài này
    private List<Integer> categoryIds; // Thể loại riêng bài này
}
