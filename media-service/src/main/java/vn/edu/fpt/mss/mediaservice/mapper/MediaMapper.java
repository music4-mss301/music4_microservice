package vn.edu.fpt.mss.mediaservice.mapper;

import vn.edu.fpt.mss.mediaservice.dto.response.MediaFileResponse;
import vn.edu.fpt.mss.mediaservice.dto.response.MediaUploadResponse;
import vn.edu.fpt.mss.mediaservice.entity.MediaFile;

public final class MediaMapper {

    private MediaMapper() {
    }

    public static MediaFileResponse toResponse(MediaFile m) {
        return new MediaFileResponse(m.getId(), m.getObjectKey(), m.getOriginalFilename(), m.getContentType(),
                m.getMediaType(), m.getFileSize(), m.getDuration(), m.getUploadedBy(), m.getCreatedAt());
    }

    public static MediaUploadResponse toUploadResponse(MediaFile m) {
        return new MediaUploadResponse(m.getId(), m.getObjectKey(), m.getObjectKey(), m.getContentType(),
                m.getMediaType(), m.getFileSize(), m.getDuration());
    }
}
