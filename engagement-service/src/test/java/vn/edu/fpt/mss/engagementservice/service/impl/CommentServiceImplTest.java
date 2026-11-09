package vn.edu.fpt.mss.engagementservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.fpt.mss.engagementservice.client.ContentClient;
import vn.edu.fpt.mss.engagementservice.client.UserClient;
import vn.edu.fpt.mss.engagementservice.dto.client.TrackSummary;
import vn.edu.fpt.mss.engagementservice.dto.client.UserSummary;
import vn.edu.fpt.mss.engagementservice.dto.request.CommentRequest;
import vn.edu.fpt.mss.engagementservice.dto.request.CommentUpdateRequest;
import vn.edu.fpt.mss.engagementservice.dto.response.CommentResponse;
import vn.edu.fpt.mss.engagementservice.entity.Comment;
import vn.edu.fpt.mss.engagementservice.exception.BusinessException;
import vn.edu.fpt.mss.engagementservice.exception.ForbiddenException;
import vn.edu.fpt.mss.engagementservice.exception.ResourceNotFoundException;
import vn.edu.fpt.mss.engagementservice.repository.CommentRepository;
import vn.edu.fpt.mss.engagementservice.security.AuthUser;
import vn.edu.fpt.mss.engagementservice.security.SecurityUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock CommentRepository commentRepository;
    @Mock ContentClient contentClient;
    @Mock UserClient userClient;
    @InjectMocks CommentServiceImpl commentService;

    @Test
    void create_whenExceedsRateLimit_throwsBusinessException() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            AuthUser user = new AuthUser(1, "listener1", "USER");
            utilities.when(SecurityUtils::requireUser).thenReturn(user);

            when(commentRepository.countByUserIdAndCreatedAtAfter(eq(1), any(LocalDateTime.class)))
                    .thenReturn(5L);

            CommentRequest req = new CommentRequest(10, "Bài hát hay quá!");
            BusinessException ex = assertThrows(BusinessException.class, () -> commentService.create(req));
            assertEquals("Bạn bình luận quá nhanh, vui lòng thử lại sau ít phút", ex.getMessage());
            verify(commentRepository, never()).save(any());
        }
    }

    @Test
    void create_whenTrackNotFound_throwsResourceNotFoundException() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            AuthUser user = new AuthUser(1, "listener1", "USER");
            utilities.when(SecurityUtils::requireUser).thenReturn(user);

            when(commentRepository.countByUserIdAndCreatedAtAfter(eq(1), any(LocalDateTime.class)))
                    .thenReturn(0L);
            when(contentClient.getTracks(List.of(999))).thenReturn(List.of());

            CommentRequest req = new CommentRequest(999, "Bài hát hay quá!");
            assertThrows(ResourceNotFoundException.class, () -> commentService.create(req));
            verify(commentRepository, never()).save(any());
        }
    }

    @Test
    void create_whenValid_savesAndReturnsTrimmedComment() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            AuthUser user = new AuthUser(1, "listener1", "USER");
            utilities.when(SecurityUtils::requireUser).thenReturn(user);

            when(commentRepository.countByUserIdAndCreatedAtAfter(eq(1), any(LocalDateTime.class)))
                    .thenReturn(1L);
            TrackSummary track = new TrackSummary(10, "Song A", "img.jpg", 200, "tracks/a.mp3", null,
                    100L, LocalDate.now(), 1, "Album A", List.of());
            when(contentClient.getTracks(List.of(10))).thenReturn(List.of(track));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(50);
                return c;
            });
            when(userClient.getUsers(List.of(1)))
                    .thenReturn(List.of(new UserSummary(1, "Listener One", "listener1", null, "USER")));

            CommentResponse res = commentService.create(new CommentRequest(10, "  Tuyệt vời!  "));
            assertEquals(50, res.id());
            assertEquals(10, res.trackId());
            assertEquals(1, res.userId());
            assertEquals("Tuyệt vời!", res.content());
        }
    }

    @Test
    void update_whenNotOwner_throwsForbiddenException() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            AuthUser anotherUser = new AuthUser(2, "listener2", "USER");
            utilities.when(SecurityUtils::requireUser).thenReturn(anotherUser);

            Comment existing = Comment.builder().id(100).trackId(10).userId(1).content("Cũ").build();
            when(commentRepository.findById(100)).thenReturn(Optional.of(existing));

            ForbiddenException ex = assertThrows(ForbiddenException.class,
                    () -> commentService.update(100, new CommentUpdateRequest("Sửa nội dung")));
            assertEquals("Bạn chỉ được sửa bình luận của mình", ex.getMessage());
            verify(commentRepository, never()).saveAndFlush(any());
        }
    }

    @Test
    void delete_whenNotOwnerAndNotAdmin_throwsForbiddenException() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            AuthUser anotherUser = new AuthUser(2, "listener2", "USER");
            utilities.when(SecurityUtils::requireUser).thenReturn(anotherUser);

            Comment existing = Comment.builder().id(100).trackId(10).userId(1).content("Bình luận").build();
            when(commentRepository.findById(100)).thenReturn(Optional.of(existing));

            assertThrows(ForbiddenException.class, () -> commentService.delete(100));
            verify(commentRepository, never()).delete(any());
        }
    }

    @Test
    void delete_whenAdminDeletesOtherUserComment_succeeds() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            AuthUser admin = new AuthUser(99, "admin", "ADMIN");
            utilities.when(SecurityUtils::requireUser).thenReturn(admin);

            Comment existing = Comment.builder().id(100).trackId(10).userId(1).content("Bình luận vi phạm").build();
            when(commentRepository.findById(100)).thenReturn(Optional.of(existing));

            commentService.delete(100);
            verify(commentRepository).delete(existing);
        }
    }
}
