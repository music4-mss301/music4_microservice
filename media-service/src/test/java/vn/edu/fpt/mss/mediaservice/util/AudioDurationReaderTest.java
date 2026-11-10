package vn.edu.fpt.mss.mediaservice.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Đọc thời lượng file âm thanh upload: .wav (Java Sound) và file hỏng. */
class AudioDurationReaderTest {

    /** Sinh một file WAV im lặng dài đúng {@code seconds} giây (8 kHz, 8 bit, mono). */
    private static byte[] silentWav(int seconds) throws Exception {
        float sampleRate = 8000f;
        byte[] pcm = new byte[(int) sampleRate * seconds];
        AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
        try (AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(pcm), format, pcm.length)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, out);
            return out.toByteArray();
        }
    }

    @Test
    void readSeconds_ofWavFile_returnsItsDuration() throws Exception {
        MockMultipartFile wav = new MockMultipartFile("file", "tone.wav", "audio/wav", silentWav(3));

        assertEquals(3, AudioDurationReader.readSeconds(wav));
    }

    @Test
    void readSeconds_ofCorruptedWav_returnsNullInsteadOfFailing() {
        MockMultipartFile broken = new MockMultipartFile("file", "broken.wav", "audio/wav", new byte[]{1, 2, 3});

        assertNull(AudioDurationReader.readSeconds(broken));
    }
}
