package vn.edu.fpt.mss.analyticsservice.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends AppException {
    public BusinessException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
