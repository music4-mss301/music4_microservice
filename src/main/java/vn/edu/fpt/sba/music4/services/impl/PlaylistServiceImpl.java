package vn.edu.fpt.sba.music4.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.sba.music4.dtos.request.AddTrackRequest;
import vn.edu.fpt.sba.music4.dtos.request.PlaylistRequest;
import vn.edu.fpt.sba.music4.dtos.response.PlaylistResponseDTO;
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

    private User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản đăng nhập."));
    }

    @Override
    public PlaylistResponseDTO createPlaylist(PlaylistRequest request) {
        User currentUser = getAuthenticatedUser();

        Playlist playlist = new Playlist();
        playlist.setName(request.name());
        playlist.setUser(currentUser);
        Playlist savedPlaylist = playlistRepository.save(playlist);
        return new PlaylistResponseDTO(
                savedPlaylist.getId(),
                savedPlaylist.getName(),
                currentUser.getId(),
                currentUser.getUsername()
        );
    }

    @Override
    public List<PlaylistResponseDTO> getMyPlaylists() {
        User currentUser = getAuthenticatedUser();
        List<Playlist> playlists = playlistRepository.findByUserId(currentUser.getId());

        return playlists.stream()
                .map(p -> new PlaylistResponseDTO(
                        p.getId(),
                        p.getName(),
                        currentUser.getId(),
                        currentUser.getUsername()
                ))
                .toList();
    }

    @Override
    public PlaylistResponseDTO updatePlaylist(Integer id, PlaylistRequest request) {
        User currentUser = getAuthenticatedUser();
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Playlist yêu cầu."));

        if (!playlist.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa playlist này.");
        }

        playlist.setName(request.name());
        Playlist updatedPlaylist = playlistRepository.save(playlist);

        return new PlaylistResponseDTO(
                updatedPlaylist.getId(),
                updatedPlaylist.getName(),
                currentUser.getId(),
                currentUser.getUsername()
        );
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
    public void addTrackToPlaylist(Integer playlistId, AddTrackRequest request) {
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

        playlistTrackRepository.save(playlistTrack);
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