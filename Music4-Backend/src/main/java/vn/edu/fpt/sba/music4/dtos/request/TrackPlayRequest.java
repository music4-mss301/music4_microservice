package vn.edu.fpt.sba.music4.dtos.request;

import jakarta.validation.constraints.NotNull;

public record TrackPlayRequest (
        @NotNull(message = "Track ID is required")
        Integer trackId,
        Integer userId
){
}
