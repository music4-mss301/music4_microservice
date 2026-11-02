package vn.edu.fpt.mss.userservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Base exception: mỗi lớp con gắn sẵn một HTTP status để GlobalExceptionHandler trả về. */
@Getter
public class AppException extends RuntimeException {
    private final HttpStatus status;

    public AppException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
