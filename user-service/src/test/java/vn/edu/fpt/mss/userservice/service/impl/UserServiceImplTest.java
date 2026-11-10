package vn.edu.fpt.mss.userservice.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.fpt.mss.userservice.client.ContentClient;
import vn.edu.fpt.mss.userservice.dto.client.ArtistProfileResponse;
import vn.edu.fpt.mss.userservice.dto.request.UpdateProfileRequest;
import vn.edu.fpt.mss.userservice.entity.User;
import vn.edu.fpt.mss.userservice.event.EventPublisher;
import vn.edu.fpt.mss.userservice.exception.BusinessException;
import vn.edu.fpt.mss.userservice.exception.ForbiddenException;
import vn.edu.fpt.mss.userservice.repository.UserRepository;
import vn.edu.fpt.mss.userservice.security.AuthUser;
import vn.edu.fpt.mss.userservice.service.RefreshTokenService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Business rule của quản lý người dùng (không cần DB: repository và content-service giả lập bằng Mockito). */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private ContentClient contentClient;
    @InjectMocks
    private UserServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void login(Integer id, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthUser(id, "user" + id, role), null, List.of()));
    }

    private User user(Integer id, String role) {
        return User.builder().id(id).name("User " + id).email("u" + id + "@music4.local")
                .userName("user" + id).password("hash").role(role).build();
    }

    // ---------------------------------------------------------------- Rule: đổi quyền

    @Test
    void changeRole_ofYourself_isConflict() {
        login(1, "ADMIN");
        when(userRepository.findById(1)).thenReturn(Optional.of(user(1, "ADMIN")));

        assertThrows(BusinessException.class, () -> service.changeRole(1, "LISTENER"));
    }

    @Test
    void changeRole_demotingLastActiveAdmin_isConflict() {
        login(1, "ADMIN");
        when(userRepository.findById(2)).thenReturn(Optional.of(user(2, "ADMIN")));
        when(userRepository.countByRoleAndEnabledTrueAndIsDeletedFalse("ADMIN")).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.changeRole(2, "LISTENER"));
    }

    @Test
    void changeRole_listenerToArtist_createsArtistProfile() {
        login(1, "ADMIN");
        User listener = user(3, "LISTENER");
        when(userRepository.findById(3)).thenReturn(Optional.of(listener));
        when(contentClient.createArtistProfile(any())).thenReturn(new ArtistProfileResponse(7, 3, "User 3"));

        service.changeRole(3, "ARTIST");

        assertEquals("ARTIST", listener.getRole());
        assertEquals(7, listener.getArtistId());
        verify(refreshTokenService).revokeAllOf("user3");
    }

    // ---------------------------------------------------------------- Rule: khóa / xóa

    @Test
    void changeStatus_lockingYourself_isConflict() {
        login(2, "LISTENER");
        when(userRepository.findById(2)).thenReturn(Optional.of(user(2, "LISTENER")));

        assertThrows(BusinessException.class, () -> service.changeStatus(2, false));
        verify(eventPublisher, never()).publish(anyString(), any());
    }

    @Test
    void delete_lastActiveAdmin_isConflict() {
        login(1, "ADMIN");
        when(userRepository.findById(2)).thenReturn(Optional.of(user(2, "ADMIN")));
        when(userRepository.countByRoleAndEnabledTrueAndIsDeletedFalse("ADMIN")).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.delete(2));
    }

    // ---------------------------------------------------------------- Rule: sửa hồ sơ

    @Test
    void updateProfile_ofAnotherUser_isForbidden() {
        login(2, "LISTENER");

        assertThrows(ForbiddenException.class,
                () -> service.updateProfile(3, new UpdateProfileRequest("New name", null, null, null)));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void updateProfile_withEmailUsedByAnotherUser_isConflict() {
        login(2, "LISTENER");
        when(userRepository.findById(2)).thenReturn(Optional.of(user(2, "LISTENER")));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("taken@music4.local", 2)).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> service.updateProfile(2, new UpdateProfileRequest(null, "taken@music4.local", null, null)));
    }
}
