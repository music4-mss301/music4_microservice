package vn.edu.fpt.mss.contentservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.fpt.mss.contentservice.dto.request.CreateArtistProfileRequest;
import vn.edu.fpt.mss.contentservice.entity.Artist;
import vn.edu.fpt.mss.contentservice.event.EventPublisher;
import vn.edu.fpt.mss.contentservice.exception.BusinessException;
import vn.edu.fpt.mss.contentservice.repository.AlbumRepository;
import vn.edu.fpt.mss.contentservice.repository.ArtistRepository;
import vn.edu.fpt.mss.contentservice.repository.TrackArtistRepository;
import vn.edu.fpt.mss.contentservice.repository.TrackRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtistServiceImplTest {

    @Mock
    ArtistRepository artistRepository;

    @Mock
    AlbumRepository albumRepository;

    @Mock
    TrackRepository trackRepository;

    @Mock
    TrackArtistRepository trackArtistRepository;

    @Mock
    EventPublisher eventPublisher;

    @InjectMocks
    ArtistServiceImpl artistService;

    /** Rule: Không xóa nghệ sĩ đang còn bài hát -> ném 409 BusinessException */
    @Test
    void delete_whenArtistStillHasTracks_throwsBusinessException() {
        Artist artist = Artist.builder().id(1).name("Artist 1").isDeleted(false).build();
        when(artistRepository.findByIdAndIsDeletedFalse(1)).thenReturn(Optional.of(artist));
        when(trackArtistRepository.countByArtistId(1)).thenReturn(4L);

        assertThrows(BusinessException.class, () -> artistService.delete(1));

        assertFalse(artist.getIsDeleted());
        verify(eventPublisher, never()).publish(any(), any());
    }

    /** Rule: Mỗi userId chỉ có tối đa 1 hồ sơ nghệ sĩ -> ném 409 BusinessException */
    @Test
    void createProfile_whenUserAlreadyHasProfile_throwsBusinessException() {
        when(artistRepository.existsByUserId(2)).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> artistService.createProfile(new CreateArtistProfileRequest(2, "New Artist", true)));

        verify(artistRepository, never()).save(any());
    }
}