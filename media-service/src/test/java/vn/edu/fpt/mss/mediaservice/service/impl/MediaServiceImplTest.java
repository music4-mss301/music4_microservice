package vn.edu.fpt.mss.mediaservice.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.fpt.mss.mediaservice.dto.response.MediaUploadResponse;
import vn.edu.fpt.mss.mediaservice.entity.MediaFile;
import vn.edu.fpt.mss.mediaservice.exception.ForbiddenException;
import vn.edu.fpt.mss.mediaservice.repository.MediaFileRepository;
import vn.edu.fpt.mss.mediaservice.security.AuthUser;
import vn.edu.fpt.mss.mediaservice.service.StorageService;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock
    private MediaFileRepository repository;

    @Mock
    private StorageService storage;

    @InjectMocks
    private MediaServiceImpl mediaService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockCurrentUser(int id, String username, String role) {
        AuthUser user = new AuthUser(id, username, role);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /** Rule: Chỉ ARTIST hoặc ADMIN mới được upload audio (tracks) -> LISTENER upload ném 403 ForbiddenException */
    @Test
    @DisplayName("Ném ForbiddenException khi người nghe thông thường (LISTENER) cố tình upload nhạc")
    void upload_whenListenerUploadsAudio_throwsForbiddenException() {
        mockCurrentUser(3, "listener", "LISTENER");

        byte[] mp3Bytes = new byte[]{'I', 'D', '3', 0, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "test.mp3", "audio/mpeg", mp3Bytes);

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> mediaService.upload(file, "tracks", null));

        assertTrue(ex.getMessage().contains("Chỉ nghệ sĩ hoặc ADMIN được upload nhạc"));
        verify(storage, never()).put(anyString(), any());
        verify(repository, never()).save(any());
    }

    /** Rule: Nghệ sĩ (ARTIST) upload audio hợp lệ -> upload lên S3 và lưu Database thành công */
    @Test
    @DisplayName("Upload nhạc thành công khi người dùng có quyền ARTIST")
    void upload_whenArtistUploadsValidAudio_success() {
        mockCurrentUser(2, "artist", "ARTIST");

        byte[] mp3Bytes = new byte[]{'I', 'D', '3', 0, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "hit.mp3", "audio/mpeg", mp3Bytes);

        when(repository.existsByObjectKey(anyString())).thenReturn(false);
        when(storage.bucket()).thenReturn("music4-bucket");

        MediaFile savedEntity = MediaFile.builder()
                .id(100L)
                .objectKey("tracks/hit.mp3")
                .originalFilename("hit.mp3")
                .contentType("audio/mpeg")
                .mediaType("AUDIO")
                .fileSize(file.getSize())
                .bucketName("music4-bucket")
                .uploadedBy(2)
                .build();
        when(repository.save(any(MediaFile.class))).thenReturn(savedEntity);

        MediaUploadResponse response = mediaService.upload(file, "tracks", null);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("tracks/hit.mp3", response.objectKey());
        verify(storage, times(1)).put(anyString(), eq(file));
        verify(repository, times(1)).save(any(MediaFile.class));
    }

    /** Rule: Người dùng bất kỳ (kể cả LISTENER) vẫn được phép upload ảnh (ví dụ avatar) */
    @Test
    @DisplayName("Upload ảnh thành công cho mọi người dùng (kể cả LISTENER)")
    void upload_whenListenerUploadsImage_success() {
        mockCurrentUser(3, "listener", "LISTENER");

        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", jpegBytes);

        when(repository.existsByObjectKey(anyString())).thenReturn(false);
        when(storage.bucket()).thenReturn("music4-bucket");

        MediaFile savedEntity = MediaFile.builder()
                .id(200L)
                .objectKey("images/avatar.jpg")
                .originalFilename("avatar.jpg")
                .contentType("image/jpeg")
                .mediaType("IMAGE")
                .fileSize(file.getSize())
                .bucketName("music4-bucket")
                .uploadedBy(3)
                .build();
        when(repository.save(any(MediaFile.class))).thenReturn(savedEntity);

        MediaUploadResponse response = mediaService.upload(file, "images", null);

        assertNotNull(response);
        assertEquals(200L, response.id());
        verify(storage, times(1)).put(anyString(), eq(file));
    }

    /** Rule: Chỉ chủ sở hữu file hoặc ADMIN mới được xóa file -> Người khác xóa ném 403 Forbidden */
    @Test
    @DisplayName("Ném ForbiddenException khi người dùng cố tình xóa file của người khác")
    void delete_whenUserIsNotOwnerNorAdmin_throwsForbiddenException() {
        mockCurrentUser(99, "other_user", "LISTENER");

        MediaFile existingFile = MediaFile.builder()
                .id(10L)
                .objectKey("tracks/song.mp3")
                .uploadedBy(2) // Thuộc về user id 2
                .build();

        when(repository.findById(10L)).thenReturn(Optional.of(existingFile));

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> mediaService.delete(10L));

        assertTrue(ex.getMessage().contains("không có quyền"));
        verify(storage, never()).delete(anyString());
        verify(repository, never()).delete(any());
    }

    /** Rule: Chủ sở hữu file xóa file của mình -> Xóa file trên S3 và xóa bản ghi DB */
    @Test
    @DisplayName("Xóa file thành công khi người dùng chính là chủ sở hữu file")
    void delete_whenUserIsOwner_success() {
        mockCurrentUser(2, "artist", "ARTIST");

        MediaFile existingFile = MediaFile.builder()
                .id(10L)
                .objectKey("tracks/my_song.mp3")
                .uploadedBy(2) // Trùng với user id đang đăng nhập
                .build();

        when(repository.findById(10L)).thenReturn(Optional.of(existingFile));

        assertDoesNotThrow(() -> mediaService.delete(10L));

        verify(storage, times(1)).delete("tracks/my_song.mp3");
        verify(repository, times(1)).delete(existingFile);
    }
}
