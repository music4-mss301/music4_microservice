package vn.edu.fpt.mss.engagementservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.fpt.mss.engagementservice.client.ContentClient;
import vn.edu.fpt.mss.engagementservice.dto.client.ArtistSummary;
import vn.edu.fpt.mss.engagementservice.exception.BusinessException;
import vn.edu.fpt.mss.engagementservice.repository.FavoriteRepository;
import vn.edu.fpt.mss.engagementservice.repository.FollowRepository;
import vn.edu.fpt.mss.engagementservice.repository.RatingRepository;
import vn.edu.fpt.mss.engagementservice.security.AuthUser;
import vn.edu.fpt.mss.engagementservice.security.SecurityUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryServiceImplTest {

    @Mock FollowRepository followRepository;
    @Mock FavoriteRepository favoriteRepository;
    @Mock RatingRepository ratingRepository;
    @Mock ContentClient contentClient;
    @InjectMocks LibraryServiceImpl libraryService;

    @Test
    void toggleFollow_whenFollowSelf_throwsBusinessException() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {

            AuthUser mockUser = new AuthUser(1, "artist_user", "ARTIST");
            utilities.when(SecurityUtils::requireUser).thenReturn(mockUser);

            when(followRepository.findByUserIdAndArtistId(1, 99)).thenReturn(Optional.empty());

            ArtistSummary mockArtist = new ArtistSummary(99, 1, "My Stage Name", "img.jpg");
            when(contentClient.getArtist(99)).thenReturn(mockArtist);

            BusinessException ex = assertThrows(BusinessException.class, () -> libraryService.toggleFollow(99));
            assertEquals("Không thể tự theo dõi chính mình", ex.getMessage());
            verify(followRepository, never()).save(any());
        }
    }
}