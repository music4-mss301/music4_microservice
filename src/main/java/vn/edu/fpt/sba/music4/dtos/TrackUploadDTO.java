package vn.edu.fpt.sba.music4.dtos;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrackUploadDTO {

    private String title;          // Tên bài hát
    private Integer duration;      // Thời lượng bài hát (tính bằng giây)
    private Integer albumId;       // ID của Album (nếu có, không có thì để null)[cite: 1]

    // Nhận danh sách các ID Thể loại vì 1 bài hát có thể thuộc nhiều thể loại[cite: 1]
    private List<Integer> categoryIds;

    // Nhận danh sách các ID Ca sĩ (Người đầu tiên là Main, còn lại là Feat/Collab)[cite: 1]
    private List<Integer> artistIds;
}