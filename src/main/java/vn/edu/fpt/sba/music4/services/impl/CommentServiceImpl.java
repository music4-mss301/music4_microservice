package vn.edu.fpt.sba.music4.services.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.fpt.sba.music4.dtos.request.CommentRequest;
import vn.edu.fpt.sba.music4.dtos.request.UpdateCommentRequest;
import vn.edu.fpt.sba.music4.dtos.response.CommentResponseDTO;
import vn.edu.fpt.sba.music4.entities.Comment;
import vn.edu.fpt.sba.music4.repositories.ArtistRepository;
import vn.edu.fpt.sba.music4.repositories.CommentRepository;
import vn.edu.fpt.sba.music4.repositories.TrackRepository;
import vn.edu.fpt.sba.music4.repositories.UserRepository;
import vn.edu.fpt.sba.music4.services.ICommentService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements ICommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final TrackRepository trackRepository;
    private final ArtistRepository artistRepository;

    @Override
    @Transactional
    public void addComment(CommentRequest request) {
        Comment comment = new Comment();
        comment.setTrack(trackRepository.getReferenceById(request.trackId()));
        comment.setContent(request.content());
        comment.setCreatedAt(LocalDateTime.now());

        if (request.userId() != null) {
            comment.setUser(userRepository.getReferenceById(request.userId()));
        } else if (request.artistId() != null) {
            comment.setArtist(artistRepository.getReferenceById(request.artistId()));
        } else {
            throw new RuntimeException("Phải có thông tin User hoặc Artist bình luận.");
        }

        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public List<CommentResponseDTO> getCommentsByTrack(Integer trackId) {
        List<Comment> comments = commentRepository.findByTrackIdOrderByCreatedAtDesc(trackId);

        return comments.stream().map(c -> {
            String name = "Ẩn danh";
            String img = null;
            String role = "UNKNOWN";
            Integer uId = null;

            if (c.getUser() != null) {
                name = c.getUser().getName();
                img = c.getUser().getImg();
                role = "USER";
                uId = c.getUser().getId(); // Lấy ID của User bình luận
            } else if (c.getArtist() != null) {
                name = c.getArtist().getName();
                img = c.getArtist().getImg();
                role = "ARTIST";
                uId = c.getArtist().getId(); // Lấy ID của Artist bình luận
            }

            return new CommentResponseDTO(c.getId(), uId, name, img, role, c.getContent(), c.getCreatedAt());
        }).toList();
    }

    @Override
    @Transactional
    public void deleteComment(Integer commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new RuntimeException("Không tìm thấy bình luận cần xóa.");
        }
        commentRepository.deleteById(commentId);
    }

    @Override
    @Transactional
    public void updateComment(Integer commentId, UpdateCommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận cần cập nhật."));

        comment.setContent(request.content());
        commentRepository.save(comment);
    }
}