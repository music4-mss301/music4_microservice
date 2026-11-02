package vn.edu.fpt.mss.mediaservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.mss.mediaservice.dto.response.MediaFileResponse;
import vn.edu.fpt.mss.mediaservice.dto.response.MediaUploadResponse;
import vn.edu.fpt.mss.mediaservice.dto.response.PageResponse;
import vn.edu.fpt.mss.mediaservice.entity.MediaFile;
import vn.edu.fpt.mss.mediaservice.enums.MediaType;
import vn.edu.fpt.mss.mediaservice.exception.BusinessException;
import vn.edu.fpt.mss.mediaservice.exception.ForbiddenException;
import vn.edu.fpt.mss.mediaservice.exception.ResourceNotFoundException;
import vn.edu.fpt.mss.mediaservice.mapper.MediaMapper;
import vn.edu.fpt.mss.mediaservice.repository.MediaFileRepository;
import vn.edu.fpt.mss.mediaservice.security.AuthUser;
import vn.edu.fpt.mss.mediaservice.security.SecurityUtils;
import vn.edu.fpt.mss.mediaservice.service.MediaService;
import vn.edu.fpt.mss.mediaservice.service.StorageService;
import vn.edu.fpt.mss.mediaservice.util.AudioDurationReader;
import vn.edu.fpt.mss.mediaservice.util.FileValidator;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MediaFileRepository repository;
    private final StorageService storage;

    /** Rule: chỉ ARTIST/ADMIN upload audio; định dạng + magic bytes + dung lượng được kiểm tra; key unique. */
    @Override
    @Transactional
    public MediaUploadResponse upload(MultipartFile file, String type, Integer uploadedBy) {
        MediaType mediaType = MediaType.fromApiType(type);
        SecurityUtils.currentUser().ifPresent(user -> {
            if (mediaType == MediaType.AUDIO && !user.isArtist() && !user.isAdmin()) {
                throw new ForbiddenException("Chỉ nghệ sĩ hoặc ADMIN được upload nhạc");
            }
        });
        FileValidator.validate(file, mediaType);
        String key = mediaType.folder() + "/" + UUID.randomUUID() + extensionOf(file.getOriginalFilename());
        if (repository.existsByObjectKey(key)) {
            throw new BusinessException("Trùng object key, vui lòng thử lại");
        }
        Integer duration = mediaType == MediaType.AUDIO ? AudioDurationReader.readSeconds(file) : null;
        storage.put(key, file);
        Integer owner = uploadedBy != null ? uploadedBy : SecurityUtils.currentUserIdOrNull();
        MediaFile saved = repository.save(MediaFile.builder()
                .objectKey(key).originalFilename(file.getOriginalFilename()).contentType(file.getContentType())
                .mediaType(mediaType.name()).fileSize(file.getSize()).bucketName(storage.bucket())
                .duration(duration).uploadedBy(owner).build());
        return MediaMapper.toUploadResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MediaFileResponse get(Long id) {
        return MediaMapper.toResponse(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MediaFileResponse> list(String type, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("id").descending());
        var result = StringUtils.hasText(type)
                ? repository.findByMediaType(MediaType.fromApiType(type).name(), pageable)
                : repository.findAll(pageable);
        return PageResponse.of(result, MediaMapper::toResponse);
    }

    /** Thay nội dung file nhưng giữ nguyên key, để mọi nơi đang tham chiếu key không phải sửa. */
    @Override
    @Transactional
    public MediaFileResponse replace(Long id, MultipartFile file) {
        MediaFile media = find(id);
        requireOwnerOrAdmin(media);
        MediaType type = MediaType.valueOf(media.getMediaType());
        FileValidator.validate(file, type);
        storage.put(media.getObjectKey(), file);
        media.setOriginalFilename(file.getOriginalFilename());
        media.setContentType(file.getContentType());
        media.setFileSize(file.getSize());
        if (type == MediaType.AUDIO) {
            media.setDuration(AudioDurationReader.readSeconds(file));
        }
        return MediaMapper.toResponse(media);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MediaFile media = find(id);
        requireOwnerOrAdmin(media);
        storage.delete(media.getObjectKey());
        repository.delete(media);
    }

    /** Gọi khi nhận content.track.deleted. Key không có trong DB thì bỏ qua (idempotent). */
    @Override
    @Transactional
    public int deleteByKeys(List<String> keys) {
        List<MediaFile> files = repository.findByObjectKeyIn(keys.stream().filter(Objects::nonNull).toList());
        files.forEach(f -> storage.delete(f.getObjectKey()));
        repository.deleteAll(files);
        return files.size();
    }

    private MediaFile find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy file id=" + id));
    }

    private void requireOwnerOrAdmin(MediaFile media) {
        AuthUser user = SecurityUtils.requireUser();
        if (!user.isAdmin() && !Objects.equals(user.id(), media.getUploadedBy())) {
            throw new ForbiddenException("Bạn không có quyền với file này");
        }
    }

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        String ext = filename.substring(filename.lastIndexOf('.')).toLowerCase();
        return ext.matches("\\.[a-z0-9]{1,5}") ? ext : "";
    }
}
