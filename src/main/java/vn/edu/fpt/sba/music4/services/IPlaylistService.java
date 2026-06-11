package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.request.AddTrackRequest;
import vn.edu.fpt.sba.music4.dtos.request.PlaylistRequest;
import vn.edu.fpt.sba.music4.entities.Playlist;
import vn.edu.fpt.sba.music4.entities.PlaylistTrack;

import java.util.List;

public interface IPlaylistService {
    Playlist createPlaylist(PlaylistRequest request);
    List<Playlist> getMyPlaylists();
    Playlist updatePlaylist(Integer id, PlaylistRequest request);
    void deletePlaylist(Integer id);

    PlaylistTrack addTrackToPlaylist(Integer playlistId, AddTrackRequest request);
    void removeTrackFromPlaylist(Integer playlistId, Integer trackId);
}