package vn.edu.fpt.mss.contentservice.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.fpt.mss.contentservice.client.MediaClient;
import vn.edu.fpt.mss.contentservice.dto.event.TrackEventPayload;
import vn.edu.fpt.mss.contentservice.dto.request.AlbumBulkRequest;
import vn.edu.fpt.mss.contentservice.dto.request.TrackBulkItem;
import vn.edu.fpt.mss.contentservice.entity.Artist;
import vn.edu.fpt.mss.contentservice.entity.Track;
import vn.edu.fpt.mss.contentservice.entity.TrackArtist;
import vn.edu.fpt.mss.contentservice.event.EventPublisher;
import vn.edu.fpt.mss.contentservice.event.EventTypes;
import vn.edu.fpt.mss.contentservice.exception.BadRequestException;
import vn.edu.fpt.mss.contentservice.exception.BusinessException;
import vn.edu.fpt.mss.contentservice.exception.ForbiddenException;
import vn.edu.fpt.mss.contentservice.repository.AlbumRepository;
import vn.edu.fpt.mss.contentservice.repository.ArtistRepository;
import vn.edu.fpt.mss.contentservice.repository.CategoryRepository;
import vn.edu.fpt.mss.contentservice.repository.TrackRepository;
import vn.edu.fpt.mss.contentservice.security.AuthUser;
import vn.edu.fpt.mss.contentservice.service.ArtistService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Business rule của bài hát (không cần DB hay RabbitMQ: repository và client được giả lập bằng Mockito). */
@ExtendWith(MockitoExtension.class)
class TrackServiceImplTest {

    @Mock
    TrackRepository trackRepository;
    @Mock
    AlbumRepository albumRepository;
    @Mock
    ArtistRepository artistRepository;
    @Mock
    CategoryRepository categoryRepository;
    @Mock
    ArtistService artistService;
    @Mock
    MediaClient mediaClient;
    @Mock
    EventPublisher eventPublisher;

    @InjectMocks
    TrackServiceImpl trackService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void login(Integer id, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthUser(id, "user" + id, role), null, List.of()));
    }

    private Artist artist(Integer id) {
        return Artist.builder().id(id).name("Artist " + id).build();
    }

    /** Track.builder() không giữ giá trị mặc định của danh sách, nên dựng bằng constructor rỗng. */
    private Track trackOf(Integer id, Artist main, String filePath) {
        Track track = new Track();
        track.setId(id);
        track.setName("Track " + id);
        track.setFilePath(filePath);
        track.setPreviewPath(filePath);
        track.getTrackArtists().add(TrackArtist.builder().track(track).artist(main).role(TrackArtist.MAIN).build());
        return track;
    }

    private AlbumBulkRequest bulk(Integer albumId, TrackBulkItem item) {
        return new AlbumBulkRequest(albumId, "covers/cover.jpg", List.of(item));
    }

    // ---------------------------------------------------------------- Rule: tải nhiều bài (bulk-json)

    @Test
    void saveBulk_asAdminWithoutArtistOrAlbum_isBadRequest() {
        login(1, "ADMIN");
        TrackBulkItem item = new TrackBulkItem("Bai 1", "tracks/a.mp3", 120, null, null, List.of(1));

        assertThrows(BadRequestException.class, () -> trackService.saveBulk(bulk(null, item)));
        verify(trackRepository, never()).save(any());
    }

    @Test
    void saveBulk_withAudioFileAlreadyUsedByAnotherTrack_isConflict() {
        login(1, "ADMIN");
        when(artistService.findActive(9)).thenReturn(artist(9));
        when(trackRepository.existsByFilePath("tracks/taken.mp3")).thenReturn(true);
        TrackBulkItem item = new TrackBulkItem("Bai 1", "tracks/taken.mp3", 120, 9, null, List.of(1));

        assertThrows(BusinessException.class, () -> trackService.saveBulk(bulk(null, item)));
        verify(trackRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- Rule: xóa bài hát

    @Test
    void delete_byAnotherArtist_isForbidden() {
        login(2, "ARTIST");
        Track track = trackOf(10, artist(1), "tracks/a.mp3");
        when(trackRepository.findById(10)).thenReturn(Optional.of(track));
        when(artistService.requireCurrentArtist()).thenReturn(artist(2));

        assertThrows(ForbiddenException.class, () -> trackService.delete(10));
        verify(trackRepository, never()).delete(any(Track.class));
        verify(eventPublisher, never()).publish(any(), any());
    }

    @Test
    void delete_whenAudioFileIsSharedWithAnotherTrack_doesNotAskMediaToDeleteIt() {
        login(1, "ADMIN");
        Track track = trackOf(10, artist(1), "tracks/shared.mp3");
        when(trackRepository.findById(10)).thenReturn(Optional.of(track));
        when(trackRepository.existsByFilePathAndIdNot("tracks/shared.mp3", 10)).thenReturn(true);

        trackService.delete(10);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(eq(EventTypes.TRACK_DELETED), payload.capture());
        TrackEventPayload event = (TrackEventPayload) payload.getValue();
        assertNull(event.filePath());
        assertNull(event.previewPath());
    }

    @Test
    void delete_whenAudioFileIsOnlyUsedHere_sendsTheKeysSoMediaCanDeleteThem() {
        login(1, "ADMIN");
        Track track = trackOf(10, artist(1), "tracks/own.mp3");
        when(trackRepository.findById(10)).thenReturn(Optional.of(track));
        when(trackRepository.existsByFilePathAndIdNot("tracks/own.mp3", 10)).thenReturn(false);

        trackService.delete(10);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(eq(EventTypes.TRACK_DELETED), payload.capture());
        assertEquals("tracks/own.mp3", ((TrackEventPayload) payload.getValue()).filePath());
    }
}
