package vn.edu.fpt.mss.contentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.mss.contentservice.client.MediaClient;
import vn.edu.fpt.mss.contentservice.dto.request.AlbumRequest;
import vn.edu.fpt.mss.contentservice.dto.response.AlbumDetailResponse;
import vn.edu.fpt.mss.contentservice.dto.response.AlbumResponse;
import vn.edu.fpt.mss.contentservice.dto.response.PageResponse;
import vn.edu.fpt.mss.contentservice.entity.Album;
import vn.edu.fpt.mss.contentservice.entity.Artist;
import vn.edu.fpt.mss.contentservice.event.EventPublisher;
import vn.edu.fpt.mss.contentservice.event.EventTypes;
import vn.edu.fpt.mss.contentservice.exception.BadRequestException;
import vn.edu.fpt.mss.contentservice.exception.BusinessException;
import vn.edu.fpt.mss.contentservice.exception.ForbiddenException;
import vn.edu.fpt.mss.contentservice.exception.ResourceNotFoundException;
import vn.edu.fpt.mss.contentservice.mapper.ContentMapper;
import vn.edu.fpt.mss.contentservice.repository.AlbumRepository;
import vn.edu.fpt.mss.contentservice.repository.TrackRepository;
import vn.edu.fpt.mss.contentservice.security.SecurityUtils;
import vn.edu.fpt.mss.contentservice.service.AlbumService;
import vn.edu.fpt.mss.contentservice.service.ArtistService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlbumServiceImpl implements AlbumService {

    private final AlbumRepository albumRepository;
    private final TrackRepository trackRepository;
    private final ArtistService artistService;
    private final MediaClient mediaClient;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AlbumResponse> getAll(int page, int size) {
        return PageResponse.of(albumRepository.findAllBy(PageRequest.of(page, Math.min(size, 100),
                Sort.by("uploadDate").descending())), ContentMapper::toAlbumResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AlbumDetailResponse getDetail(Integer id) {
        Album album = find(id);
        return new AlbumDetailResponse(ContentMapper.toAlbumResponse(album),
                trackRepository.findByAlbumIdOrderByIdAsc(id).stream().map(ContentMapper::toTrackResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlbumResponse> getByArtist(Integer artistId) {
        return albumRepository.findByArtistIdOrderByUploadDateDesc(artistId).stream()
                .map(ContentMapper::toAlbumResponse).toList();
    }

    /** Rule: tên album không trùng trong cùng một nghệ sĩ. */
    @Override
    @Transactional
    public AlbumResponse create(AlbumRequest req) {
        Artist owner = resolveOwner(req.artistId());
        if (albumRepository.existsByArtistIdAndNameIgnoreCase(owner.getId(), req.name().trim())) {
            throw new BusinessException("Nghệ sĩ đã có album tên '" + req.name().trim() + "'");
        }
        Album album = albumRepository.save(Album.builder().name(req.name().trim()).img(req.img()).artist(owner).build());
        artistService.refreshCounters(owner);
        eventPublisher.publish(EventTypes.ALBUM_CREATED, ContentMapper.toAlbumEvent(album));
        return ContentMapper.toAlbumResponse(album);
    }

    @Override
    @Transactional
    public AlbumResponse createWithCover(String title, MultipartFile cover, Integer artistId) {
        if (!StringUtils.hasText(title)) {
            throw new BadRequestException("Tên album không được để trống");
        }
        String key = null;
        if (cover != null && !cover.isEmpty()) {
            key = mediaClient.upload(cover, "images", SecurityUtils.currentUserIdOrNull()).objectKey();
        }
        return create(new AlbumRequest(title, key, artistId));
    }

    @Override
    @Transactional
    public AlbumResponse update(Integer id, AlbumRequest req) {
        Album album = find(id);
        requireOwner(album);
        String name = req.name().trim();
        if (albumRepository.existsByArtistIdAndNameIgnoreCaseAndIdNot(album.getArtist().getId(), name, id)) {
            throw new BusinessException("Nghệ sĩ đã có album tên '" + name + "'");
        }
        album.setName(name);
        if (req.img() != null) {
            album.setImg(req.img());
        }
        eventPublisher.publish(EventTypes.ALBUM_UPDATED, ContentMapper.toAlbumEvent(album));
        return ContentMapper.toAlbumResponse(album);
    }

    /** Rule: không xóa album còn bài hát (409). */
    @Override
    @Transactional
    public void delete(Integer id) {
        Album album = find(id);
        requireOwner(album);
        if (trackRepository.countByAlbumId(id) > 0) {
            throw new BusinessException("Album còn bài hát, hãy xóa hoặc gỡ bài hát khỏi album trước");
        }
        Artist owner = album.getArtist();
        var payload = ContentMapper.toAlbumEvent(album);
        albumRepository.delete(album);
        albumRepository.flush();
        artistService.refreshCounters(owner);
        eventPublisher.publish(EventTypes.ALBUM_DELETED, payload);
    }

    private Album find(Integer id) {
        return albumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy album id=" + id));
    }

    private Artist resolveOwner(Integer artistId) {
        if (SecurityUtils.isAdmin()) {
            if (artistId == null) {
                throw new BadRequestException("ADMIN phải chỉ định artistId");
            }
            return artistService.findActive(artistId);
        }
        return artistService.requireCurrentArtist();
    }

    private void requireOwner(Album album) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        if (!ArtistServiceImpl.sameArtist(artistService.requireCurrentArtist(), album.getArtist())) {
            throw new ForbiddenException("Chỉ chủ album hoặc ADMIN được thao tác");
        }
    }
}
