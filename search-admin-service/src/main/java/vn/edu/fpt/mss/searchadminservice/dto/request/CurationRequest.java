package vn.edu.fpt.mss.searchadminservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Ghim các bài lên đầu kết quả khi người dùng tìm đúng từ khóa (vd: chiến dịch quảng bá). */
public record CurationRequest(@NotBlank(message = "Từ khóa không được để trống") @Size(max = 100) String keyword,
                              @NotEmpty(message = "Phải ghim ít nhất 1 bài") @Size(max = 10, message = "Tối đa 10 bài")
                              List<Integer> trackIds,
                              @Size(max = 255) String note) {
}
