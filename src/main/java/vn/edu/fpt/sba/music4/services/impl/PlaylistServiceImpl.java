package vn.edu.fpt.sba.music4.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.sba.music4.dtos.request.AddTrackRequest;
import vn.edu.fpt.sba.music4.dtos.request.PlaylistRequest;
import vn.edu.fpt.sba.music4.entities.*;
import vn.edu.fpt.sba.music4.repositories.*;
import vn.edu.fpt.sba.music4.services.IPlaylistService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaylistServiceImpl implements IPlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistTrackRepository playlistTrackRepository;
    private final UserRepository userRepository;
    private final TrackRepository trackRepository;

    // Lấy thông tin User đang thao tác dựa trên Spring Security + JWT
    private User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản đăng nhập."));
    }

    @Override
    public Playlist createPlaylist(PlaylistRequest request) {
        User currentUser = getAuthenticatedUser();

        Playlist playlist = new Playlist();
        playlist.setName(request.name()); // Gọi trường từ record
        playlist.setUser(currentUser);

        return playlistRepository.save(playlist);
    }

    @Override
    public List<Playlist> getMyPlaylists() {
        User currentUser = getAuthenticatedUser();
        return playlistRepository.findByUserId(currentUser.getId());
    }

    @Override
    public Playlist updatePlaylist(Integer id, PlaylistRequest request) {
        User currentUser = getAuthenticatedUser();
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Playlist yêu cầu."));

        // Bảo mật: Kiểm tra quyền sở hữu trước khi sửa
        if (!playlist.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa playlist này.");
        }

        playlist.setName(request.name());
        return playlistRepository.save(playlist);
    }

    @Override
    public void deletePlaylist(Integer id) {
        User currentUser = getAuthenticatedUser();
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Playlist yêu cầu."));

        // Bảo mật: Kiểm tra quyền sở hữu trước khi xóa
        if (!playlist.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền xóa playlist này.");
        }

        playlistRepository.delete(playlist);
    }

    @Override
    public PlaylistTrack addTrackToPlaylist(Integer playlistId, AddTrackRequest request) {
        User currentUser = getAuthenticatedUser();
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Playlist yêu cầu."));

        if (!playlist.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa playlist này.");
        }

        // Kiểm tra xem bài hát đã có trong playlist chưa (Ràng buộc UQ_Playlist_Track trong DB)
        if (playlistTrackRepository.existsByPlaylistIdAndTrackId(playlistId, request.trackId())) {
            throw new RuntimeException("Bài hát này đã tồn tại trong playlist.");
        }

        Track track = trackRepository.findById(request.trackId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài hát yêu cầu."));

        PlaylistTrack playlistTrack = new PlaylistTrack();
        playlistTrack.setPlaylist(playlist);
        playlistTrack.setTrack(track);
        playlistTrack.setAddedAt(LocalDateTime.now());

        return playlistTrackRepository.save(playlistTrack);
    }

    @Override
    @Transactional
    public void removeTrackFromPlaylist(Integer playlistId, Integer trackId) {
        User currentUser = getAuthenticatedUser();
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Playlist yêu cầu."));

        if (!playlist.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa playlist này.");
        }

        PlaylistTrack playlistTrack = playlistTrackRepository.findByPlaylistIdAndTrackId(playlistId, trackId)
                .orElseThrow(() -> new RuntimeException("Bài hát không tồn tại trong playlist này."));

        playlistTrackRepository.delete(playlistTrack);
    }
}