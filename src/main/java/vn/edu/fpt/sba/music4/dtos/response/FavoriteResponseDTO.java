package vn.edu.fpt.sba.music4.dtos.response;

import java.time.LocalDateTime;

public record FavoriteResponseDTO(
        Integer favoriteId,
        Integer trackId,
        String trackName,
        String artistName,
        String img,
        LocalDateTime likedAt
) {}