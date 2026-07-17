package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.request.AddTrackRequest;
import vn.edu.fpt.sba.music4.dtos.request.PlaylistRequest;
import vn.edu.fpt.sba.music4.dtos.response.PlaylistResponseDTO;
import vn.edu.fpt.sba.music4.dtos.response.TrackResponseDTO; // 🟢 Gom import lên đây cho sạch code

import java.util.List;

public interface IPlaylistService {

    PlaylistResponseDTO createPlaylist(PlaylistRequest request);

    List<PlaylistResponseDTO> getMyPlaylists();

    PlaylistResponseDTO updatePlaylist(Integer id, PlaylistRequest request);

    PlaylistResponseDTO getPlaylistById(Integer id);

    PlaylistResponseDTO uploadPlaylistImage(Integer id, org.springframework.web.multipart.MultipartFile file);

    List<TrackResponseDTO> getTracksByPlaylistId(Integer playlistId);

    void deletePlaylist(Integer id);

    void addTrackToPlaylist(Integer playlistId, AddTrackRequest request);

    void removeTrackFromPlaylist(Integer playlistId, Integer trackId);
}