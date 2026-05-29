package vn.edu.fpt.sba.music4.dtos;

import lombok.*;
import java.util.List;

@Data // 🔥 Tự động sinh Getter, Setter, toString...
@NoArgsConstructor // 🔥 Bắt buộc phải có Constructor không tham số cho @ModelAttribute
@AllArgsConstructor // 🔥 Hỗ trợ khởi tạo đầy đủ tham số
public class TrackUploadDTO {

    private String title;
    private Integer artistId;
    private Integer duration;
    private Integer albumId;
    private List<Integer> categoryIds;
    private List<Integer> artistIds;
}