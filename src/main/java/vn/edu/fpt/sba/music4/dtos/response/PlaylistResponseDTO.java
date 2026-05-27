package vn.edu.fpt.sba.music4.dtos.response;

public record PlaylistResponseDTO(Integer id,
                                  String name,
                                  Integer userId, // Chỉ lấy ID của User (nếu bạn có trường tên User, có thể thêm String userName vào đây
                                  String userName
) {
}
