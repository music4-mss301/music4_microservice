package vn.edu.fpt.sba.music4.dtos.request;

import org.springframework.web.multipart.MultipartFile;

public record UserRequest(MultipartFile img, String name, Boolean gender) {
}
