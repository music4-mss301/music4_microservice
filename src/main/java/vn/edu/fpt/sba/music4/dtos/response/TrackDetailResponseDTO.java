package vn.edu.fpt.sba.music4.dtos.response;

import java.time.LocalDate;
import java.util.List;

public record TrackDetailResponseDTO(
        Integer id,
        String name,
        String img,
        Integer duration,
        String filePath,
        String previewPath,
        LocalDate uploadDate,
        Integer viewCount,
        AlbumResponseDTO album, // Sẽ mang giá trị null nếu là bài Single lẻ
        List<TrackArtistInfo> artists, // Bóc tách sạch sẽ từ TrackArtist
        List<CategoryResponseDTO> categories // Bóc tách sạch sẽ từ TrackCategory
) {}
