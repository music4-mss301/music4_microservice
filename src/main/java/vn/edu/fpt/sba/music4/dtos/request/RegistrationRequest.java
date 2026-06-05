package vn.edu.fpt.sba.music4.dtos.request;

import org.springframework.web.multipart.MultipartFile;

public record RegistrationRequest(MultipartFile img, String name, String email, String username, String password, String role) {
}
