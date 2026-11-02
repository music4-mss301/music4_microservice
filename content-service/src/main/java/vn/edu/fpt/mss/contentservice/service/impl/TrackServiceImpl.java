package vn.edu.fpt.mss.contentservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.mss.contentservice.client.MediaClient;
import vn.edu.fpt.mss.contentservice.dto.client.MediaUploadResponse;
import vn.edu.fpt.mss.contentservice.dto.event.TrackEventPayload;
import vn.edu.fpt.mss.contentservice.dto.request.*;
import vn.edu.fpt.mss.contentservice.dto.response.*;
import vn.edu.fpt.mss.contentservice.entity.*;
import vn.edu.fpt.mss.contentservice.event.EventPublisher;
import vn.edu.fpt.mss.contentservice.event.EventTypes;
import vn.edu.fpt.mss.contentservice.exception.*;
import vn.edu.fpt.mss.contentservice.mapper.ContentMapper;
import vn.edu.fpt.mss.contentservice.repository.*;
import vn.edu.fpt.mss.contentservice.security.SecurityUtils;
import vn.edu.fpt.mss.contentservice.service.ArtistService;
import vn.edu.fpt.mss.contentservice.service.TrackService;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackServiceImpl implements TrackService {

    private static final Set<String> SORTABLE = Set.of("id", "name", "viewCount", "uploadDate");

    private final TrackRepository trackRepository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final CategoryRepository categoryRepository;
    private final ArtistService artistService;
    private final MediaClient mediaClient;
    private final EventPublisher eventPublisher;

    // ---------------------------------------------------------------- READ

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TrackResponse> getAll(int page, int size, String sort) {
        String field = SORTABLE.contains(sort) ? sort : "id";
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, field));
        return PageResponse.of(trackRepository.findAllBy(pageable), ContentMapper::toTrackResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TrackDetailResponse getDetail(Integer id) {
        return ContentMapper.toTrackDetail(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackResponse> getByAlbum(Integer albumId) {
        return trackRepository.findByAlbumIdOrderByIdAsc(albumId).stream().map(ContentMapper::toTrackResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackResponse> getByArtist(Integer artistId) {
        return trackRepository.findByArtistId(artistId).stream().map(ContentMapper::toTrackResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackResponse> getTrending(int limit) {
        return trackRepository.findTrending(PageRequest.of(0, Math.min(Math.max(limit, 1), 50))).stream()
                .map(ContentMapper::toTrackResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackResponse> getByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<Integer, Track> byId = new HashMap<>();
        trackRepository.findByIdIn(ids).forEach(t -> byId.put(t.getId(), t));
        // giữ đúng thứ tự id mà caller gửi (playlist, gợi ý...)
        return ids.stream().map(byId::get).filter(Objects::nonNull).map(ContentMapper::toTrackResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TrackEventPayload> exportForIndex(int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 200), Sort.by("id"));
        return PageResponse.of(trackRepository.findAllBy(pageable), ContentMapper::toTrackEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TrackResponse> search(String keyword, int page, int size) {
        if (!StringUtils.hasText(keyword)) {
            throw new BadRequestException("Từ khóa tìm kiếm không được để trống");
        }
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("name"));
        return PageResponse.of(trackRepository.search(keyword.trim(), pageable), ContentMapper::toTrackResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveStreamKey(Integer id, boolean preview) {
        Track track = find(id);
        String key = preview && StringUtils.hasText(track.getPreviewPath()) ? track.getPreviewPath() : track.getFilePath();
        if (!StringUtils.hasText(key)) {
            throw new ResourceNotFoundException("Bài hát chưa có file audio");
        }
        return key;
    }

    @Override
    @Transactional(readOnly = true)
    public ContentStatsResponse stats() {
        return new ContentStatsResponse(trackRepository.count(), albumRepository.count(),
                artistRepository.countByIsDeletedFalse(), categoryRepository.count(), trackRepository.sumAllViews());
    }

    // ---------------------------------------------------------------- WRITE

    /** Rule: ≥ 1 nghệ sĩ (nghệ sĩ chính = người upload), album phải của nghệ sĩ chính, tên không trùng trong album. */
    @Override
    @Transactional
    public TrackDetailResponse upload(MultipartFile audioFile, MultipartFile coverImage, TrackUploadRequest req) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BadRequestException("File âm thanh (audioFile) không được để trống");
        }
        if (coverImage == null || coverImage.isEmpty()) {
            throw new BadRequestException("Ảnh bìa (coverImage) không được để trống");
        }
        Artist main = resolveMainArtist(req.artistId());
        Album album = resolveAlbum(req.albumId(), main);
        assertNameAvailable(album, req.title(), null);

        Integer uploader = SecurityUtils.currentUserIdOrNull();
        MediaUploadResponse cover = mediaClient.upload(coverImage, "images", uploader);
        MediaUploadResponse audio = mediaClient.upload(audioFile, "tracks", uploader);

        Track track = Track.builder()
                .name(req.title().trim())
                .duration(audio.duration())
                .filePath(audio.objectKey())
                .previewPath(audio.objectKey())
                .img(cover.objectKey())
                .album(album)
                .build();
        applyArtists(track, main, req.artistIds());
        applyCategories(track, req.categoryIds());
        track = trackRepository.save(track);
        afterTrackChange(track, album, null);
        eventPublisher.publish(EventTypes.TRACK_CREATED, ContentMapper.toTrackEvent(track));
        return ContentMapper.toTrackDetail(track);
    }

    @Override
    public UploadTempResponse uploadTemp(MultipartFile file, String type) {
        if (!"images".equalsIgnoreCase(type) && !"tracks".equalsIgnoreCase(type)) {
            throw new BadRequestException("type chỉ nhận 'images' hoặc 'tracks'");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File upload không được để trống");
        }
        MediaUploadResponse res = mediaClient.upload(file, type.toLowerCase(), SecurityUtils.currentUserIdOrNull());
        return new UploadTempResponse(res.objectKey(), res.duration());
    }

    @Override
    @Transactional
    public List<TrackResponse> saveBulk(AlbumBulkRequest req) {
        Artist currentMain = resolveMainArtist(null);
        Album album = resolveAlbum(req.albumId(), currentMain);
        Set<String> namesInRequest = new HashSet<>();
        List<Track> saved = new ArrayList<>();
        for (TrackBulkItem item : req.tracks()) {
            Artist main = SecurityUtils.isAdmin() && item.artistId() != null
                    ? artistService.findActive(item.artistId()) : currentMain;
            if (!namesInRequest.add(item.title().trim().toLowerCase())) {
                throw new BusinessException("Tên bài '" + item.title() + "' bị trùng trong danh sách gửi lên");
            }
            assertNameAvailable(album, item.title(), null);
            Track track = Track.builder()
                    .name(item.title().trim())
                    .duration(item.duration())
                    .filePath(item.audioFileKey())
                    .previewPath(item.audioFileKey())
                    .img(req.coverImageKey())
                    .album(album)
                    .build();
            applyArtists(track, main, item.artistIds());
            applyCategories(track, item.categoryIds());
            saved.add(trackRepository.save(track));
        }
        saved.forEach(t -> afterTrackChange(t, album, null));
        saved.forEach(t -> eventPublisher.publish(EventTypes.TRACK_CREATED, ContentMapper.toTrackEvent(t)));
        return saved.stream().map(ContentMapper::toTrackResponse).toList();
    }

    /** Rule: chỉ nghệ sĩ chính của bài hoặc ADMIN được sửa. */
    @Override
    @Transactional
    public TrackDetailResponse update(Integer id, TrackUpdateRequest req) {
        Track track = find(id);
        Artist main = requireOwner(track);
        Album oldAlbum = track.getAlbum();
        Set<Artist> affected = new HashSet<>(currentArtists(track));

        if (req.albumId() != null) {
            track.setAlbum(req.albumId() == -1 ? null : resolveAlbum(req.albumId(), main));
        }
        String newName = StringUtils.hasText(req.title()) ? req.title().trim() : track.getName();
        assertNameAvailable(track.getAlbum(), newName, track.getId());
        track.setName(newName);
        if (req.duration() != null) {
            track.setDuration(req.duration());
        }
        if (req.img() != null) {
            track.setImg(req.img());
        }
        if (req.categoryIds() != null) {
            track.getTrackCategories().clear();
            trackRepository.flush();
            applyCategories(track, req.categoryIds());
        }
        if (req.artistIds() != null) {
            track.getTrackArtists().clear();
            trackRepository.flush();
            applyArtists(track, main, req.artistIds());
        }
        track = trackRepository.save(track);
        affected.addAll(currentArtists(track));
        afterTrackChange(track, track.getAlbum(), oldAlbum);
        affected.forEach(artistService::refreshCounters);
        eventPublisher.publish(EventTypes.TRACK_UPDATED, ContentMapper.toTrackEvent(track));
        return ContentMapper.toTrackDetail(track);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Track track = find(id);
        requireOwner(track);
        TrackEventPayload payload = ContentMapper.toTrackEvent(track);
        List<Artist> artists = currentArtists(track);
        Album album = track.getAlbum();
        trackRepository.delete(track);
        trackRepository.flush();
        artists.forEach(artistService::refreshCounters);
        if (album != null) {
            album.setTrackTotal(trackRepository.countByAlbumId(album.getId()));
        }
        // engagement dọn playlist/favorite/rating; media xóa file S3; search xóa document
        eventPublisher.publish(EventTypes.TRACK_DELETED, payload);
    }

    @Override
    @Transactional
    public void incrementView(Integer trackId) {
        if (trackRepository.incrementViewCount(trackId) == 0) {
            log.warn("Bỏ qua lượt nghe cho track không tồn tại id={}", trackId);
        }
    }

    // ---------------------------------------------------------------- helpers

    private Track find(Integer id) {
        return trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài hát id=" + id));
    }

    /** ARTIST: luôn là chính mình. ADMIN: phải chỉ định artistId. */
    private Artist resolveMainArtist(Integer requestedArtistId) {
        if (SecurityUtils.isAdmin()) {
            if (requestedArtistId == null) {
                throw new BadRequestException("ADMIN phải chỉ định artistId của nghệ sĩ chính");
            }
            return artistService.findActive(requestedArtistId);
        }
        return artistService.requireCurrentArtist();
    }

    private Album resolveAlbum(Integer albumId, Artist main) {
        if (albumId == null) {
            return null;
        }
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy album id=" + albumId));
        if (!ArtistServiceImpl.sameArtist(album.getArtist(), main)) {
            throw new BusinessException("Album không thuộc nghệ sĩ chính của bài hát");
        }
        return album;
    }

    private void assertNameAvailable(Album album, String name, Integer excludeTrackId) {
        if (album == null || !StringUtils.hasText(name)) {
            return;
        }
        boolean taken = excludeTrackId == null
                ? trackRepository.existsByAlbumIdAndNameIgnoreCase(album.getId(), name.trim())
                : trackRepository.existsByAlbumIdAndNameIgnoreCaseAndIdNot(album.getId(), name.trim(), excludeTrackId);
        if (taken) {
            throw new BusinessException("Album đã có bài hát tên '" + name.trim() + "'");
        }
    }

    private void applyArtists(Track track, Artist main, List<Integer> featIds) {
        track.getTrackArtists().add(TrackArtist.builder().track(track).artist(main).role(TrackArtist.MAIN).build());
        if (featIds == null) {
            return;
        }
        new LinkedHashSet<>(featIds).stream()
                .filter(fid -> !Objects.equals(fid, main.getId()))
                .map(artistService::findActive)
                .forEach(a -> track.getTrackArtists()
                        .add(TrackArtist.builder().track(track).artist(a).role(TrackArtist.FEAT).build()));
    }

    private void applyCategories(Track track, List<Integer> categoryIds) {
        if (categoryIds == null) {
            return;
        }
        new LinkedHashSet<>(categoryIds).forEach(cid -> {
            Category category = categoryRepository.findById(cid)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thể loại id=" + cid));
            track.getTrackCategories().add(TrackCategory.builder().track(track).category(category).build());
        });
    }

    private Artist requireOwner(Track track) {
        Artist main = track.getTrackArtists().stream()
                .filter(ta -> TrackArtist.MAIN.equals(ta.getRole()))
                .map(TrackArtist::getArtist).findFirst()
                .orElseThrow(() -> new BusinessException("Bài hát không có nghệ sĩ chính"));
        if (!SecurityUtils.isAdmin()) {
            Artist me = artistService.requireCurrentArtist();
            if (!ArtistServiceImpl.sameArtist(me, main)) {
                throw new ForbiddenException("Chỉ nghệ sĩ chính của bài hoặc ADMIN được thao tác");
            }
        }
        return main;
    }

    private List<Artist> currentArtists(Track track) {
        return track.getTrackArtists().stream().map(TrackArtist::getArtist).toList();
    }

    private void afterTrackChange(Track track, Album newAlbum, Album oldAlbum) {
        trackRepository.flush();
        currentArtists(track).forEach(artistService::refreshCounters);
        if (newAlbum != null) {
            newAlbum.setTrackTotal(trackRepository.countByAlbumId(newAlbum.getId()));
        }
        if (oldAlbum != null && (newAlbum == null || !Objects.equals(oldAlbum.getId(), newAlbum.getId()))) {
            oldAlbum.setTrackTotal(trackRepository.countByAlbumId(oldAlbum.getId()));
        }
    }
}
