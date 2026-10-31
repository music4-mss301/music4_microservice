package vn.edu.fpt.mss.contentservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.fpt.mss.contentservice.client.MediaClient;
import vn.edu.fpt.mss.contentservice.dto.request.AlbumRequest;
import vn.edu.fpt.mss.contentservice.entity.Album;
import vn.edu.fpt.mss.contentservice.entity.Artist;
import vn.edu.fpt.mss.contentservice.event.EventPublisher;
import vn.edu.fpt.mss.contentservice.exception.BusinessException;
import vn.edu.fpt.mss.contentservice.exception.ForbiddenException;
import vn.edu.fpt.mss.contentservice.repository.AlbumRepository;
import vn.edu.fpt.mss.contentservice.repository.TrackRepository;
import vn.edu.fpt.mss.contentservice.service.ArtistService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlbumServiceImplTest {

    @Mock
    AlbumRepository albumRepository;

    @Mock
    TrackRepository trackRepository;

    @Mock
    ArtistService artistService;

    @Mock
    MediaClient mediaClient;

    @Mock
    EventPublisher eventPublisher;

    @InjectMocks
    AlbumServiceImpl albumService;

    /** Rule: Tên album không được trùng trong cùng một nghệ sĩ -> ném 409 BusinessException */
    @Test
    void create_whenDuplicateNameForSameArtist_throwsBusinessException() {
        Artist artist = Artist.builder().id(10).name("Sơn Tùng").build();
        when(artistService.requireCurrentArtist()).thenReturn(artist);
        when(albumRepository.existsByArtistIdAndNameIgnoreCase(10, "Sky Tour")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> albumService.create(new AlbumRequest("Sky Tour", null, null)));

        verify(albumRepository, never()).save(any());
    }

    /** Rule: Chỉ chủ album hoặc ADMIN mới được xóa/sửa album -> ném 403 ForbiddenException */
    @Test
    void delete_whenCurrentArtistIsNotOwner_throwsForbiddenException() {
        Artist owner = Artist.builder().id(10).name("Chủ Album").build();
        Artist otherArtist = Artist.builder().id(99).name("Nghệ Sĩ Khác").build();
        Album album = Album.builder().id(5).name("Album A").artist(owner).build();

        when(albumRepository.findById(5)).thenReturn(Optional.of(album));
        when(artistService.requireCurrentArtist()).thenReturn(otherArtist);

        assertThrows(ForbiddenException.class, () -> albumService.delete(5));

        verify(albumRepository, never()).delete(any());
    }

    /** Rule: Không xóa album khi còn bài hát bên trong -> ném 409 BusinessException */
    @Test
    void delete_whenAlbumStillHasTracks_throwsBusinessException() {
        Artist owner = Artist.builder().id(10).name("Chủ Album").build();
        Album album = Album.builder().id(5).name("Album A").artist(owner).build();

        when(albumRepository.findById(5)).thenReturn(Optional.of(album));
        when(artistService.requireCurrentArtist()).thenReturn(owner);
        when(trackRepository.countByAlbumId(5)).thenReturn(2);

        assertThrows(BusinessException.class, () -> albumService.delete(5));

        verify(albumRepository, never()).delete(any());
    }
}