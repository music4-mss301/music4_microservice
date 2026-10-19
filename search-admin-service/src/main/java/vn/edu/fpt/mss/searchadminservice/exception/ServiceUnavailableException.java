package vn.edu.fpt.mss.searchadminservice.exception;

import org.springframework.http.HttpStatus;

public class ServiceUnavailableException extends AppException {
    public ServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
