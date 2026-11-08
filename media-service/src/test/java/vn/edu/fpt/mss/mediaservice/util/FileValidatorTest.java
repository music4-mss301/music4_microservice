package vn.edu.fpt.mss.mediaservice.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import vn.edu.fpt.mss.mediaservice.enums.MediaType;
import vn.edu.fpt.mss.mediaservice.exception.BadRequestException;

import static org.junit.jupiter.api.Assertions.*;

class FileValidatorTest {

    /** Rule: File không được rỗng (null hoặc 0 bytes) -> ném 400 BadRequestException */
    @Test
    @DisplayName("Ném BadRequestException khi file upload rỗng (0 bytes)")
    void validate_whenFileIsEmpty_throwsBadRequestException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.mp3", "audio/mpeg", new byte[0]
        );

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> FileValidator.validate(emptyFile, MediaType.AUDIO));

        assertTrue(ex.getMessage().contains("không được để trống"));
    }

    /** Rule: Dung lượng file không được vượt quá giới hạn (Audio tối đa 50MB, Image tối đa 5MB) */
    @Test
    @DisplayName("Ném BadRequestException khi dung lượng file vượt quá 50MB đối với audio")
    void validate_whenFileSizeExceedsLimit_throwsBadRequestException() {
        // Giả lập file dung lượng 51MB (51 * 1024 * 1024 bytes) nhưng chỉ cấp phát mảng nhỏ để tiết kiệm RAM
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.mp3", "audio/mpeg", new byte[16]) {
            @Override
            public long getSize() {
                return 51L * 1024 * 1024; // 51MB > 50MB
            }
        };

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> FileValidator.validate(largeFile, MediaType.AUDIO));

        assertTrue(ex.getMessage().contains("vượt quá 50MB"));
    }

    /** Rule: Chặn file giả mạo định dạng - Magic Bytes không khớp (ví dụ đổi file EXE thành .mp3) */
    @Test
    @DisplayName("Ném BadRequestException khi file khai báo audio/mpeg nhưng nội dung là file EXE (MZ header)")
    void validate_whenFakeAudioWithExeMagicBytes_throwsBadRequestException() {
        // Header của file EXE DOS/PE luôn bắt đầu bằng 'M', 'Z' (0x4D, 0x5A)
        byte[] exeBytes = new byte[]{'M', 'Z', 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        MockMultipartFile fakeAudio = new MockMultipartFile(
                "file", "virus.mp3", "audio/mpeg", exeBytes
        );

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> FileValidator.validate(fakeAudio, MediaType.AUDIO));

        assertTrue(ex.getMessage().contains("Nội dung file không khớp định dạng khai báo"));
    }

    /** Rule: File MP3 hợp lệ với ID3 tag header ('I', 'D', '3') -> kiểm tra thành công không lỗi */
    @Test
    @DisplayName("Kiểm tra thành công khi upload file MP3 hợp lệ có ID3 tag header")
    void validate_whenValidMp3File_success() {
        // Header hợp lệ của file MP3 ID3v2: 'I', 'D', '3' (0x49, 0x44, 0x33)
        byte[] mp3Bytes = new byte[]{'I', 'D', '3', 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        MockMultipartFile validMp3 = new MockMultipartFile(
                "file", "song.mp3", "audio/mpeg", mp3Bytes
        );

        assertDoesNotThrow(() -> FileValidator.validate(validMp3, MediaType.AUDIO));
    }

    /** Rule: File JPEG hợp lệ với SOI header (0xFF, 0xD8, 0xFF) -> kiểm tra thành công */
    @Test
    @DisplayName("Kiểm tra thành công khi upload ảnh JPEG hợp lệ")
    void validate_whenValidJpegImage_success() {
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01};
        MockMultipartFile validJpeg = new MockMultipartFile(
                "file", "cover.jpg", "image/jpeg", jpegBytes
        );

        assertDoesNotThrow(() -> FileValidator.validate(validJpeg, MediaType.IMAGE));
    }

    /** Rule: Định dạng Content-Type không nằm trong danh sách hỗ trợ (ví dụ application/pdf) */
    @Test
    @DisplayName("Ném BadRequestException khi định dạng content-type không được hỗ trợ")
    void validate_whenContentTypeUnsupported_throwsBadRequestException() {
        byte[] dummyBytes = new byte[]{'%', 'P', 'D', 'F', 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", dummyBytes
        );

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> FileValidator.validate(pdfFile, MediaType.AUDIO));

        assertTrue(ex.getMessage().contains("không được hỗ trợ"));
    }
}
