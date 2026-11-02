package vn.edu.fpt.mss.mediaservice.enums;

import java.util.Set;

/** folder = prefix trên S3. type trên API giữ như cũ: images | tracks. */
public enum MediaType {
    IMAGE("images", 5L * 1024 * 1024, Set.of("image/jpeg", "image/png", "image/webp", "image/gif")),
    AUDIO("tracks", 50L * 1024 * 1024, Set.of("audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", "audio/wave"));

    private final String folder;
    private final long maxBytes;
    private final Set<String> contentTypes;

    MediaType(String folder, long maxBytes, Set<String> contentTypes) {
        this.folder = folder;
        this.maxBytes = maxBytes;
        this.contentTypes = contentTypes;
    }

    public String folder() {
        return folder;
    }

    public long maxBytes() {
        return maxBytes;
    }

    public Set<String> contentTypes() {
        return contentTypes;
    }

    public static MediaType fromApiType(String type) {
        if ("images".equalsIgnoreCase(type) || "image".equalsIgnoreCase(type)) {
            return IMAGE;
        }
        if ("tracks".equalsIgnoreCase(type) || "audio".equalsIgnoreCase(type)) {
            return AUDIO;
        }
        throw new IllegalArgumentException("type chỉ nhận 'images' hoặc 'tracks'");
    }
}
