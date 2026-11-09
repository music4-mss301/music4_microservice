package vn.edu.fpt.mss.mediaservice.util;

import com.mpatric.mp3agic.Mp3File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Đọc thời lượng mp3 (giây) giống monolith. Không đọc được thì trả null, không chặn upload. */
@Slf4j
public final class AudioDurationReader {

    private AudioDurationReader() {
    }

    public static Integer readSeconds(MultipartFile file) {
        File temp = null;
        try {
            temp = Files.createTempFile("upload-track-", ".mp3").toFile();
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return (int) new Mp3File(temp).getLengthInSeconds();
        } catch (Exception e) {
            log.warn("Không đọc được thời lượng file {}: {}", file.getOriginalFilename(), e.getMessage());
            return null;
        } finally {
            if (temp != null && !temp.delete()) {
                temp.deleteOnExit();
            }
        }
    }
}
