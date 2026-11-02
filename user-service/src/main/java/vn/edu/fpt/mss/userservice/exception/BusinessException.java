package vn.edu.fpt.mss.userservice.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends AppException {
    public BusinessException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
