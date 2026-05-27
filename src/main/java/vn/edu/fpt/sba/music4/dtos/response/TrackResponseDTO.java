package vn.edu.fpt.sba.music4.dtos.response;

import java.time.LocalDate;

public record TrackResponseDTO(
        Integer id,
        String name,
        String img,
        Integer duration,
        String previewPath, // Giúp Frontend gắn vào thẻ <audio> để phát thử nhạc
        Integer viewCount,
        LocalDate uploadDate
) {
}
