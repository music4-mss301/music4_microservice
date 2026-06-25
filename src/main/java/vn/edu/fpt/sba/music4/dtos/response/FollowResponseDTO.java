package vn.edu.fpt.sba.music4.dtos.response;

import java.time.LocalDateTime;

public record FollowResponseDTO(
        Integer followId,
        Integer artistId,
        String artistName,
        String artistImg,
        LocalDateTime followedAt
) {}