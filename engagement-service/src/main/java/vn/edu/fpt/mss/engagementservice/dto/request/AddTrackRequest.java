package vn.edu.fpt.mss.engagementservice.dto.request;

import jakarta.validation.constraints.NotNull;

public record AddTrackRequest(@NotNull(message = "trackId không được để trống") Integer trackId) {
}
