package vn.edu.fpt.mss.mediaservice.util;

import com.mpatric.mp3agic.Mp3File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Đọc thời lượng mp3 (giây) giống monolith. Không đọc được thì trả null, không chặn upload. */
@Slf4j
public final class AudioDurationReader {

    private AudioDurationReader() {
    }

    /** WAV: thời lượng = số frame / tần số lấy mẫu (Java Sound, không cần thư viện ngoài). */
    private static Integer wavSeconds(File wavFile) throws Exception {
        try (AudioInputStream audio = AudioSystem.getAudioInputStream(wavFile)) {
            AudioFormat format = audio.getFormat();
            long frames = audio.getFrameLength();
            if (frames < 0 || format.getFrameRate() <= 0) {
                return null;
            }
            return (int) Math.round(frames / format.getFrameRate());
        }
    }

    public static Integer readSeconds(MultipartFile file) {
        File temp = null;
        try {
            String name = file.getOriginalFilename();
            boolean wav = name != null && name.toLowerCase().endsWith(".wav");
            temp = Files.createTempFile("upload-track-", wav ? ".wav" : ".mp3").toFile();
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return wav ? wavSeconds(temp) : (int) new Mp3File(temp).getLengthInSeconds();
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
