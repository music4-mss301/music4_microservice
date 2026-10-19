package vn.edu.fpt.mss.mediaservice.util;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.mss.mediaservice.enums.MediaType;
import vn.edu.fpt.mss.mediaservice.exception.BadRequestException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Rule: kiểm tra cả content-type khai báo LẪN magic bytes thật của file
 * (chặn trường hợp đổi đuôi .exe thành .mp3). Giới hạn dung lượng theo loại.
 */
public final class FileValidator {

    private FileValidator() {
    }

    public static void validate(MultipartFile file, MediaType type) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File không được để trống");
        }
        if (file.getSize() > type.maxBytes()) {
            throw new BadRequestException("File vượt quá " + type.maxBytes() / (1024 * 1024) + "MB");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!type.contentTypes().contains(contentType)) {
            throw new BadRequestException("Định dạng " + contentType + " không được hỗ trợ cho " + type.folder());
        }
        byte[] head = readHead(file);
        boolean ok = type == MediaType.IMAGE ? isImage(head) : isAudio(head);
        if (!ok) {
            throw new BadRequestException("Nội dung file không khớp định dạng khai báo");
        }
    }

    private static byte[] readHead(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] buf = new byte[12];
            int n = in.readNBytes(buf, 0, buf.length);
            return Arrays.copyOf(buf, n);
        } catch (IOException e) {
            throw new BadRequestException("Không đọc được file");
        }
    }

    private static boolean isImage(byte[] h) {
        return startsWith(h, 0xFF, 0xD8, 0xFF)                        // JPEG
                || startsWith(h, 0x89, 0x50, 0x4E, 0x47)              // PNG
                || startsWith(h, 'G', 'I', 'F', '8')                  // GIF
                || (startsWith(h, 'R', 'I', 'F', 'F') && h.length >= 12
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P'); // WEBP
    }

    private static boolean isAudio(byte[] h) {
        return startsWith(h, 'I', 'D', '3')                                       // MP3 có ID3 tag
                || (h.length >= 2 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xE0) == 0xE0) // MP3 frame sync
                || (startsWith(h, 'R', 'I', 'F', 'F') && h.length >= 12
                && h[8] == 'W' && h[9] == 'A' && h[10] == 'V' && h[11] == 'E');  // WAV
    }

    private static boolean startsWith(byte[] data, int... prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != (prefix[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }
}
