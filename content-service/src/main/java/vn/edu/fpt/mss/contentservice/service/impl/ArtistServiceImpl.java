package vn.edu.fpt.mss.contentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.fpt.mss.contentservice.dto.request.ArtistUpdateRequest;
import vn.edu.fpt.mss.contentservice.dto.request.CreateArtistProfileRequest;
import vn.edu.fpt.mss.contentservice.dto.response.*;
import vn.edu.fpt.mss.contentservice.entity.Artist;
import vn.edu.fpt.mss.contentservice.event.EventPublisher;
import vn.edu.fpt.mss.contentservice.event.EventTypes;
import vn.edu.fpt.mss.contentservice.exception.BusinessException;
import vn.edu.fpt.mss.contentservice.exception.ForbiddenException;
import vn.edu.fpt.mss.contentservice.exception.ResourceNotFoundException;
import vn.edu.fpt.mss.contentservice.mapper.ContentMapper;
import vn.edu.fpt.mss.contentservice.repository.AlbumRepository;
import vn.edu.fpt.mss.contentservice.repository.ArtistRepository;
import vn.edu.fpt.mss.contentservice.repository.TrackArtistRepository;
import vn.edu.fpt.mss.contentservice.repository.TrackRepository;
import vn.edu.fpt.mss.contentservice.security.AuthUser;
import vn.edu.fpt.mss.contentservice.security.SecurityUtils;
import vn.edu.fpt.mss.contentservice.service.ArtistService;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final TrackRepository trackRepository;
    private final TrackArtistRepository trackArtistRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<ArtistResponse> getAll() {
        return artistRepository.findByIsDeletedFalseOrderByNameAsc().stream()
                .map(ContentMapper::toArtistResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistResponse> getByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return artistRepository.findByIdIn(ids).stream().map(ContentMapper::toArtistResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ArtistResponse> search(String keyword, int page, int size) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return PageResponse.of(artistRepository.search(kw, PageRequest.of(page, Math.min(size, 100),
                Sort.by("name"))), ContentMapper::toArtistResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ArtistDetailResponse getDetail(Integer id) {
        Artist artist = findActive(id);
        return new ArtistDetailResponse(ContentMapper.toArtistResponse(artist),
                albumRepository.findByArtistIdOrderByUploadDateDesc(id).stream()
                        .map(ContentMapper::toAlbumResponse).toList(),
                trackRepository.findByArtistId(id).stream().map(ContentMapper::toTrackResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ArtistResponse getMine() {
        return ContentMapper.toArtistResponse(requireCurrentArtist());
    }

    /** Rule: chỉ chính nghệ sĩ đó hoặc ADMIN được sửa hồ sơ. */
    @Override
    @Transactional
    public ArtistResponse update(Integer id, ArtistUpdateRequest req) {
        Artist artist = findActive(id);
        SecurityUtils.requireOwnerOrAdmin(artist.getUserId());
        if (StringUtils.hasText(req.name())) {
            artist.setName(req.name().trim());
        }
        if (req.img() != null) {
            artist.setImg(req.img());
        }
        if (req.cover() != null) {
            artist.setCover(req.cover());
        }
        if (req.gender() != null) {
            artist.setGender(req.gender());
        }
        eventPublisher.publish(EventTypes.ARTIST_UPDATED, ContentMapper.toArtistEvent(artist));
        return ContentMapper.toArtistResponse(artist);
    }

    /** Rule: không xóa nghệ sĩ còn bài hát. Xóa mềm để không làm "treo" id ở service khác. */
    @Override
    @Transactional
    public void delete(Integer id) {
        Artist artist = findActive(id);
        if (trackArtistRepository.countByArtistId(id) > 0) {
            throw new BusinessException("Nghệ sĩ còn bài hát, hãy xóa hoặc chuyển bài hát trước");
        }
        artist.setIsDeleted(true);
        eventPublisher.publish(EventTypes.ARTIST_DELETED, ContentMapper.toArtistEvent(artist));
    }

    /** Gọi từ user-service khi đăng ký tài khoản nghệ sĩ. Rule: mỗi user chỉ có 1 hồ sơ. */
    @Override
    @Transactional
    public ArtistResponse createProfile(CreateArtistProfileRequest req) {
        if (artistRepository.existsByUserId(req.userId())) {
            throw new BusinessException("Tài khoản này đã có hồ sơ nghệ sĩ");
        }
        Artist artist = artistRepository.save(Artist.builder()
                .userId(req.userId()).name(req.name().trim()).gender(req.gender()).build());
        eventPublisher.publish(EventTypes.ARTIST_CREATED, ContentMapper.toArtistEvent(artist));
        return ContentMapper.toArtistResponse(artist);
    }

    @Override
    @Transactional(readOnly = true)
    public ArtistStatsResponse stats(Integer artistId) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nghệ sĩ id=" + artistId));
        return new ArtistStatsResponse(artistId, artist.getUserId(), trackRepository.findIdsByArtistId(artistId),
                trackRepository.sumViewsByArtist(artistId));
    }

    @Override
    @Transactional(readOnly = true)
    public Artist requireCurrentArtist() {
        AuthUser user = SecurityUtils.requireUser();
        return artistRepository.findByUserIdAndIsDeletedFalse(user.id())
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không phải nghệ sĩ"));
    }

    @Override
    public Artist findActive(Integer id) {
        return artistRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nghệ sĩ id=" + id));
    }

    @Override
    public void refreshCounters(Artist artist) {
        artist.setTrackTotal((int) trackArtistRepository.countByArtistId(artist.getId()));
        artist.setAlbumTotal(albumRepository.countByArtistId(artist.getId()));
    }

    static boolean sameArtist(Artist a, Artist b) {
        return a != null && b != null && Objects.equals(a.getId(), b.getId());
    }
}
