package vn.edu.fpt.sba.music4.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.request.UserRequest;
import vn.edu.fpt.sba.music4.dtos.response.PageResponseDTO;
import vn.edu.fpt.sba.music4.dtos.response.UserResponseDTO;
import vn.edu.fpt.sba.music4.services.IUserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final IUserService userService;

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponseDTO updateUser(@PathVariable Integer id, @ModelAttribute UserRequest userRequest) {
        return userService.update(id, userRequest);
    }

    @GetMapping
    public PageResponseDTO<UserResponseDTO>  getAll(Pageable pageable) {
        return PageResponseDTO.of(userService.getAll(pageable));
    }

    @GetMapping("/count")
    public int getTotalUsers() {
        return userService.getTotalUsers();
    }
}
