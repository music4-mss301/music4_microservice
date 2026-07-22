package vn.edu.fpt.sba.music4.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

@Data // 🔥 Tự động sinh Getter, Setter, toString...
@NoArgsConstructor // 🔥 Bắt buộc phải có Constructor không tham số cho @ModelAttribute
@AllArgsConstructor // 🔥 Hỗ trợ khởi tạo đầy đủ tham số
public class TrackUploadDTO {
    @NotBlank(message = "Tên bài hát không được để trống!")
    private String title;
    private Integer artistId;
    private Integer duration;
    private Integer albumId;
    @NotEmpty(message = "Bài hát phải chọn ít nhất 1 thể loại (Category)!")
    private List<Integer> categoryIds;
    private List<Integer> artistIds;

    private org.springframework.web.multipart.MultipartFile audioFile;
    private org.springframework.web.multipart.MultipartFile coverImage;
}