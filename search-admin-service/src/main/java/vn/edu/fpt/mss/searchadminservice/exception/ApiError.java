package vn.edu.fpt.mss.searchadminservice.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(Instant timestamp, int status, String error, String message, String path,
                       Map<String, String> errors) {

    public static ApiError of(HttpStatus status, String message, String path, Map<String, String> errors) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, errors);
    }

    public static ApiError of(HttpStatus status, String message, String path) {
        return of(status, message, path, null);
    }
}
