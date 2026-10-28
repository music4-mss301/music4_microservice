package vn.edu.fpt.mss.engagementservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.fpt.mss.engagementservice.client.ContentClient;
import vn.edu.fpt.mss.engagementservice.entity.Playlist;
import vn.edu.fpt.mss.engagementservice.entity.PlaylistTrack;
import vn.edu.fpt.mss.engagementservice.exception.BusinessException;
import vn.edu.fpt.mss.engagementservice.repository.PlaylistRepository;
import vn.edu.fpt.mss.engagementservice.repository.PlaylistTrackRepository;
import vn.edu.fpt.mss.engagementservice.security.AuthUser;
import vn.edu.fpt.mss.engagementservice.security.SecurityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceImplTest {

    @Mock PlaylistRepository playlistRepository;
    @Mock PlaylistTrackRepository playlistTrackRepository;
    @Mock ContentClient contentClient;
    @InjectMocks PlaylistServiceImpl playlistService;

    @Test
    void addTrack_whenTrackAlreadyInPlaylist_throwsBusinessException() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {

            AuthUser mockUser = new AuthUser(1, "user", "USER");
            utilities.when(SecurityUtils::requireUser).thenReturn(mockUser);
            utilities.when(SecurityUtils::isAdmin).thenReturn(false);

            Playlist playlist = Playlist.builder().id(10).userId(1).tracks(new ArrayList<>()).build();
            when(playlistRepository.findById(10)).thenReturn(Optional.of(playlist));

            when(playlistTrackRepository.existsByPlaylistIdAndTrackId(10, 5)).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class, () -> playlistService.addTrack(10, 5));
            assertEquals("Bài hát đã có trong playlist", ex.getMessage());
            verify(playlistRepository, never()).saveAndFlush(any());
        }
    }

    @Test
    void addTrack_whenExceedsMaxTracks_throwsBusinessException() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
            AuthUser mockUser = new AuthUser(1, "user", "USER");
            utilities.when(SecurityUtils::requireUser).thenReturn(mockUser);
            utilities.when(SecurityUtils::isAdmin).thenReturn(false);


            List<PlaylistTrack> tracks = new ArrayList<>();
            for (int i = 0; i < 500; i++) {
                tracks.add(PlaylistTrack.builder().trackId(i).build());
            }
            Playlist playlist = Playlist.builder().id(10).userId(1).tracks(tracks).build();

            when(playlistRepository.findById(10)).thenReturn(Optional.of(playlist));
            when(playlistTrackRepository.existsByPlaylistIdAndTrackId(10, 999)).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class, () -> playlistService.addTrack(10, 999));
            assertEquals("Playlist tối đa 500 bài hát", ex.getMessage());
        }
    }
}