package vn.edu.fpt.mss.mediaservice.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public interface StorageService {
    void put(String key, MultipartFile file);

    void delete(String key);

    ResponseEntity<StreamingResponseBody> stream(String key, String rangeHeader);

    ResponseEntity<StreamingResponseBody> streamPreview(String key, String rangeHeader);

    String bucket();
}
