package vn.edu.fpt.sba.music4.dtos.response;

import java.time.LocalDate;

public record AlbumResponseDTO(
        Integer id,
        String name,
        String img,
        LocalDate uploadDate,
        Integer trackTotal,
        Integer artistId  // Chỉ lấy ID của Artist thay vì cả object Artist
){
}
