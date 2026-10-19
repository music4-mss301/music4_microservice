package vn.edu.fpt.mss.contentservice.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends AppException {
    public BusinessException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
