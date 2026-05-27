package vn.edu.fpt.sba.music4.dtos.response;

public record ArtistResponseDTO(
        Integer id,
        String name,
        String img,
        String userName,
        Integer trackTotal,
        Integer albumTotal
) {
}
