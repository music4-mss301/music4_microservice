package vn.edu.fpt.sba.music4.dtos.response;

public record RatingResponseDTO(
        double averageRating,
        int myRating,
        long totalRatings
) {}