package vn.edu.fpt.sba.music4.dtos.response;

import java.time.LocalDateTime;

public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
}
