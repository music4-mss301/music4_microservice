package vn.edu.fpt.mss.engagementservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.mss.engagementservice.client.ContentClient;
import vn.edu.fpt.mss.engagementservice.dto.client.TrackSummary;
import vn.edu.fpt.mss.engagementservice.dto.request.PlaylistRequest;
import vn.edu.fpt.mss.engagementservice.dto.response.PlaylistDetailResponse;
import vn.edu.fpt.mss.engagementservice.dto.response.PlaylistResponse;
import vn.edu.fpt.mss.engagementservice.dto.response.PlaylistTrackItem;
import vn.edu.fpt.mss.engagementservice.entity.Playlist;
import vn.edu.fpt.mss.engagementservice.entity.PlaylistTrack;
import vn.edu.fpt.mss.engagementservice.exception.BusinessException;
import vn.edu.fpt.mss.engagementservice.exception.ForbiddenException;
import vn.edu.fpt.mss.engagementservice.exception.ResourceNotFoundException;
import vn.edu.fpt.mss.engagementservice.mapper.EngagementMapper;
import vn.edu.fpt.mss.engagementservice.repository.PlaylistRepository;
import vn.edu.fpt.mss.engagementservice.repository.PlaylistTrackRepository;
import vn.edu.fpt.mss.engagementservice.security.AuthUser;
import vn.edu.fpt.mss.engagementservice.security.SecurityUtils;
import vn.edu.fpt.mss.engagementservice.service.PlaylistService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaylistServiceImpl implements PlaylistService {

    static final int MAX_TRACKS_PER_PLAYLIST = 500;
    static final int MAX_PLAYLISTS_PER_USER = 50;

    private final PlaylistRepository playlistRepository;
    private final PlaylistTrackRepository playlistTrackRepository;
    private final ContentClient contentClient;

    @Override
    @Transactional(readOnly = true)
    public List<PlaylistResponse> getMine() {
        return playlistRepository.findByUserIdOrderByCreatedAtDesc(SecurityUtils.requireUser().id()).stream()
                .map(EngagementMapper::toPlaylistResponse).toList();
    }

    /** Rule: người khác chỉ thấy playlist public. */
    @Override
    @Transactional(readOnly = true)
    public List<PlaylistResponse> getByUser(Integer userId) {
        boolean self = Objects.equals(SecurityUtils.currentUserIdOrNull(), userId) || SecurityUtils.isAdmin();
        var list = self ? playlistRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : playlistRepository.findByUserIdAndIsPublicTrueOrderByCreatedAtDesc(userId);
        return list.stream().map(EngagementMapper::toPlaylistResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlaylistDetailResponse getDetail(Integer id) {
        Playlist playlist = find(id);
        if (!playlist.isPublic()) {
            requireOwner(playlist);
        }
        return toDetail(playlist);
    }

    /** Rule: tên playlist không trùng trong cùng một user, tối đa 50 playlist/user. */
    @Override
    @Transactional
    public PlaylistResponse create(PlaylistRequest req) {
        AuthUser user = SecurityUtils.requireUser();
        if (playlistRepository.countByUserId(user.id()) >= MAX_PLAYLISTS_PER_USER) {
            throw new BusinessException("Mỗi tài khoản tối đa " + MAX_PLAYLISTS_PER_USER + " playlist");
        }
        String name = req.name().trim();
        if (playlistRepository.existsByUserIdAndNameIgnoreCase(user.id(), name)) {
            throw new BusinessException("Bạn đã có playlist tên '" + name + "'");
        }
        Playlist playlist = playlistRepository.save(Playlist.builder()
                .name(name).description(req.description()).img(req.img()).userId(user.id())
                .isPublic(req.isPublic() == null || req.isPublic()).build());
        return EngagementMapper.toPlaylistResponse(playlist);
    }

    @Override
    @Transactional
    public PlaylistResponse update(Integer id, PlaylistRequest req) {
        Playlist playlist = find(id);
        requireOwner(playlist);
        String name = req.name().trim();
        if (playlistRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(playlist.getUserId(), name, id)) {
            throw new BusinessException("Bạn đã có playlist tên '" + name + "'");
        }
        playlist.setName(name);
        playlist.setDescription(req.description());
        if (req.img() != null) {
            playlist.setImg(req.img());
        }
        if (req.isPublic() != null) {
            playlist.setPublic(req.isPublic());
        }
        return EngagementMapper.toPlaylistResponse(playlist);
    }

    @Override
    @Transactional
    public PlaylistResponse updateImage(Integer id, String img) {
        Playlist playlist = find(id);
        requireOwner(playlist);
        playlist.setImg(img);
        return EngagementMapper.toPlaylistResponse(playlist);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Playlist playlist = find(id);
        requireOwner(playlist);
        playlistRepository.delete(playlist);
    }

    /** Rule: không thêm trùng bài, tối đa 500 bài, bài phải tồn tại bên content-service. */
    @Override
    @Transactional
    public PlaylistDetailResponse addTrack(Integer playlistId, Integer trackId) {
        Playlist playlist = find(playlistId);
        requireOwner(playlist);
        if (playlistTrackRepository.existsByPlaylistIdAndTrackId(playlistId, trackId)) {
            throw new BusinessException("Bài hát đã có trong playlist");
        }
        if (playlist.getTracks().size() >= MAX_TRACKS_PER_PLAYLIST) {
            throw new BusinessException("Playlist tối đa " + MAX_TRACKS_PER_PLAYLIST + " bài hát");
        }
        if (contentClient.getTracks(List.of(trackId)).isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy bài hát id=" + trackId
                    + " (hoặc dịch vụ nội dung đang bận)");
        }
        playlist.getTracks().add(PlaylistTrack.builder().playlist(playlist).trackId(trackId).build());
        playlistRepository.saveAndFlush(playlist);
        return toDetail(playlist);
    }

    @Override
    @Transactional
    public void removeTrack(Integer playlistId, Integer trackId) {
        Playlist playlist = find(playlistId);
        requireOwner(playlist);
        if (playlistTrackRepository.deleteFromPlaylist(playlistId, trackId) == 0) {
            throw new ResourceNotFoundException("Bài hát không có trong playlist");
        }
    }

    private PlaylistDetailResponse toDetail(Playlist playlist) {
        List<Integer> ids = playlist.getTracks().stream().map(PlaylistTrack::getTrackId).toList();
        Map<Integer, TrackSummary> tracks = ids.isEmpty() ? Map.of()
                : contentClient.getTracks(ids).stream()
                .collect(Collectors.toMap(TrackSummary::id, Function.identity(), (a, b) -> a));
        List<PlaylistTrackItem> items = playlist.getTracks().stream()
                .map(pt -> new PlaylistTrackItem(pt.getTrackId(), pt.getAddedAt(),
                        tracks.containsKey(pt.getTrackId()), tracks.get(pt.getTrackId())))
                .toList();
        return new PlaylistDetailResponse(EngagementMapper.toPlaylistResponse(playlist), items);
    }

    private Playlist find(Integer id) {
        return playlistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy playlist id=" + id));
    }

    private void requireOwner(Playlist playlist) {
        AuthUser user = SecurityUtils.requireUser();
        if (!user.isAdmin() && !Objects.equals(user.id(), playlist.getUserId())) {
            throw new ForbiddenException("Đây không phải playlist của bạn");
        }
    }
}
