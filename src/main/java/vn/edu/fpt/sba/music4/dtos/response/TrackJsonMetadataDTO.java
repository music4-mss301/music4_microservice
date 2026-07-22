package vn.edu.fpt.sba.music4.dtos.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TrackJsonMetadataDTO {
    @NotBlank(message = "Tên bài hát không được để trống!")
    private String title;
    @NotBlank(message = "File âm thanh không được để trống!")
    private String audioFileKey; // Nhận lại s3Key từ Bước 1
    private Integer duration;    // Nhận lại duration từ Bước 1
    private Integer artistId;    // Ca sĩ chính bài này
    private List<Integer> artistIds; // Ca sĩ feat riêng của bài này
    @NotEmpty(message = "Bài hát phải chọn ít nhất 1 thể loại (Category)!")
    private List<Integer> categoryIds; // Thể loại riêng bài này
}
