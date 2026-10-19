package vn.edu.fpt.mss.engagementservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.mss.engagementservice.client.ContentClient;
import vn.edu.fpt.mss.engagementservice.client.UserClient;
import vn.edu.fpt.mss.engagementservice.dto.client.UserSummary;
import vn.edu.fpt.mss.engagementservice.dto.request.CommentRequest;
import vn.edu.fpt.mss.engagementservice.dto.request.CommentUpdateRequest;
import vn.edu.fpt.mss.engagementservice.dto.response.CommentResponse;
import vn.edu.fpt.mss.engagementservice.dto.response.PageResponse;
import vn.edu.fpt.mss.engagementservice.entity.Comment;
import vn.edu.fpt.mss.engagementservice.exception.BusinessException;
import vn.edu.fpt.mss.engagementservice.exception.ForbiddenException;
import vn.edu.fpt.mss.engagementservice.exception.ResourceNotFoundException;
import vn.edu.fpt.mss.engagementservice.mapper.EngagementMapper;
import vn.edu.fpt.mss.engagementservice.repository.CommentRepository;
import vn.edu.fpt.mss.engagementservice.security.AuthUser;
import vn.edu.fpt.mss.engagementservice.security.SecurityUtils;
import vn.edu.fpt.mss.engagementservice.service.CommentService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    static final int MAX_COMMENTS_PER_MINUTE = 5;

    private final CommentRepository commentRepository;
    private final ContentClient contentClient;
    private final UserClient userClient;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getByTrack(Integer trackId, int page, int size) {
        Page<Comment> comments = commentRepository.findByTrackIdOrderByCreatedAtDesc(trackId,
                PageRequest.of(page, Math.min(size, 50)));
        List<Integer> userIds = comments.getContent().stream().map(Comment::getUserId).distinct().toList();
        Map<Integer, UserSummary> users = userIds.isEmpty() ? Map.of()
                : userClient.getUsers(userIds).stream()
                .collect(Collectors.toMap(UserSummary::id, Function.identity(), (a, b) -> a));
        return PageResponse.of(comments, c -> EngagementMapper.toCommentResponse(c, users.get(c.getUserId())));
    }

    /** Rule: nội dung 1-500 ký tự, bài phải tồn tại, chống spam tối đa 5 bình luận/phút. */
    @Override
    @Transactional
    public CommentResponse create(CommentRequest req) {
        AuthUser user = SecurityUtils.requireUser();
        if (commentRepository.countByUserIdAndCreatedAtAfter(user.id(), LocalDateTime.now().minusMinutes(1))
                >= MAX_COMMENTS_PER_MINUTE) {
            throw new BusinessException("Bạn bình luận quá nhanh, vui lòng thử lại sau ít phút");
        }
        if (contentClient.getTracks(List.of(req.trackId())).isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy bài hát id=" + req.trackId());
        }
        Comment comment = commentRepository.save(Comment.builder()
                .trackId(req.trackId()).userId(user.id()).content(req.content().trim()).build());
        return EngagementMapper.toCommentResponse(comment, firstUser(user.id()));
    }

    /** Rule: chỉ người viết được sửa. */
    @Override
    @Transactional
    public CommentResponse update(Integer id, CommentUpdateRequest req) {
        Comment comment = find(id);
        if (!Objects.equals(comment.getUserId(), SecurityUtils.requireUser().id())) {
            throw new ForbiddenException("Bạn chỉ được sửa bình luận của mình");
        }
        comment.setContent(req.content().trim());
        commentRepository.saveAndFlush(comment);
        return EngagementMapper.toCommentResponse(comment, firstUser(comment.getUserId()));
    }

    /** Rule: người viết hoặc ADMIN được xóa. */
    @Override
    @Transactional
    public void delete(Integer id) {
        Comment comment = find(id);
        SecurityUtils.requireOwnerOrAdmin(comment.getUserId());
        commentRepository.delete(comment);
    }

    private Comment find(Integer id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bình luận id=" + id));
    }

    private UserSummary firstUser(Integer userId) {
        return userClient.getUsers(List.of(userId)).stream().findFirst().orElse(null);
    }
}
