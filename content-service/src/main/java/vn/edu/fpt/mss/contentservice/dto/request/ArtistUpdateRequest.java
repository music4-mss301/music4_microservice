package vn.edu.fpt.mss.contentservice.dto.request;

import jakarta.validation.constraints.Size;

public record ArtistUpdateRequest(@Size(min = 1, max = 100) String name, String img, String cover, Boolean gender) {
}
