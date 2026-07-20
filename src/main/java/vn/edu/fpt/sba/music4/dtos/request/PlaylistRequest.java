package vn.edu.fpt.sba.music4.dtos.request;

public record PlaylistRequest(
        String name,
        String description,
        Integer userId
) {
}