package vn.edu.fpt.sba.music4.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.sba.music4.dtos.request.AddTrackRequest;
import vn.edu.fpt.sba.music4.dtos.request.PlaylistRequest;
import vn.edu.fpt.sba.music4.dtos.response.PlaylistResponseDTO;
import vn.edu.fpt.sba.music4.dtos.response.TrackResponseDTO;
import vn.edu.fpt.sba.music4.entities.*;
import vn.edu.fpt.sba.music4.repositories.*;
import vn.edu.fpt.sba.music4.services.IPlaylistService;
import vn.edu.fpt.sba.music4.util.AWSUtil;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaylistServiceImpl implements IPlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistTrackRepository playlistTrackRepository;
    private final UserRepository userRepository;
    private final TrackRepository trackRepository;
    private final AWSUtil aWSUtil;

    private User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản đăng nhập."));
    }

    @Override
    public PlaylistResponseDTO getPlaylistById(Integer id) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Playlist yêu cầu."));

        return new PlaylistResponseDTO(
                playlist.getId(),
                playlist.getName(),
                playlist.getUser().getId(),
                playlist.getUser().getUsername(),
                playlist.getDescription(),
                playlist.getImg()
        );
    }

    @Override
    public List<TrackResponseDTO> getTracksByPlaylistId(Integer playlistId) {
        if (!playlistRepository.existsById(playlistId)) {
            throw new RuntimeException("Không tìm thấy Playlist yêu cầu.");
        }

        List<PlaylistTrack> playlistTracks = playlistTrackRepository.findByPlaylistId(playlistId);

        return playlistTracks.stream()
                .map(pt -> {
                    Track t = pt.getTrack();

                    return new TrackResponseDTO(
                            t.getId(),
                            t.getName(),
                            t.getAlbum() != null ? t.getAlbum().getName() : null,
                            t.getImg(),
                            t.getDuration(),
                            t.getFilePath(),
                            t.getPreviewPath(),
                            t.getViewCount(),
                            t.getUploadDate(),
                            0,
                            Collections.emptyList()
                    );
                })
                .toList();
    }

    @Override
    public PlaylistResponseDTO createPlaylist(PlaylistRequest request) {
        User currentUser = getAuthenticatedUser();

        Playlist playlist = new Playlist();
        playlist.setName(request.name());
        playlist.setDescription(request.description());
        playlist.setUser(currentUser);

        Playlist savedPlaylist = playlistRepository.save(playlist);

        return new PlaylistResponseDTO(
                savedPlaylist.getId(),
                savedPlaylist.getName(),
                currentUser.getId(),
                currentUser.getUsername(),
                savedPlaylist.getDescription(),
                savedPlaylist.getImg()
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
                        currentUser.getUsername(),
                        p.getDescription(),
                        p.getImg()
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
        playlist.setDescription(request.description());

        Playlist updatedPlaylist = playlistRepository.save(playlist);

        return new PlaylistResponseDTO(
                updatedPlaylist.getId(),
                updatedPlaylist.getName(),
                currentUser.getId(),
                currentUser.getUsername(),
                updatedPlaylist.getDescription(),
                updatedPlaylist.getImg()
        );
    }

    @Override
    public void deletePlaylist(Integer id) {
        User currentUser = getAuthenticatedUser();
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Playlist yêu cầu."));

        if (!playlist.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền xóa playlist này.");
        }

        playlistRepository.delete(playlist);
    }

    @Override
    @Transactional
    public void addTrackToPlaylist(Integer playlistId, AddTrackRequest request) {
        User currentUser = getAuthenticatedUser();
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Playlist yêu cầu."));

        if (!playlist.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa playlist này.");
        }

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

    @Override
    @Transactional
    public PlaylistResponseDTO uploadPlaylistImage(Integer id, MultipartFile file) {
        User currentUser = getAuthenticatedUser();

        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Playlist yêu cầu."));

        if (!playlist.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa playlist này.");
        }

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn một file ảnh hợp lệ.");
        }

        try {
            String s3Key = aWSUtil.uploadFile(file, "playlists");
            playlist.setImg(s3Key);
            Playlist updatedPlaylist = playlistRepository.save(playlist);

            return new PlaylistResponseDTO(
                    updatedPlaylist.getId(),
                    updatedPlaylist.getName(),
                    currentUser.getId(),
                    currentUser.getUsername(),
                    updatedPlaylist.getDescription(),
                    updatedPlaylist.getImg()
            );
        } catch (java.io.IOException e) {
            throw new RuntimeException("Upload image failed", e);
        }
    }
}