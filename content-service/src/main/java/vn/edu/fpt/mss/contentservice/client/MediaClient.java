package vn.edu.fpt.mss.contentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.mss.contentservice.dto.client.MediaUploadResponse;

/** content-service không giữ credentials S3: mọi file đi qua media-service. */
@FeignClient(name = "media-service", url = "${services.media-service.url:}",
        fallbackFactory = MediaClientFallbackFactory.class)
public interface MediaClient {

    @PostMapping(value = "/internal/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    MediaUploadResponse upload(@RequestPart("file") MultipartFile file,
                               @RequestParam("type") String type,
                               @RequestParam(value = "uploadedBy", required = false) Integer uploadedBy);
}
