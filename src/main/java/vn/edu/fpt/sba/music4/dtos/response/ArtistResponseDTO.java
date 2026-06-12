package vn.edu.fpt.sba.music4.dtos.response;

public record ArtistResponseDTO(
        Integer id,
        String name,
        String img,
        String cover,
        String email,
        String userName,
        Integer trackTotal,
        Integer albumTotal
) {
    public ArtistResponseDTO(
            Integer id,
            String name,
            String img,
            String userName,
            Integer trackTotal,
            Integer albumTotal
    ) {
        this(
                id,
                name,
                img,
                null,
                null,
                userName,
                trackTotal,
                albumTotal
        );
    }
}


