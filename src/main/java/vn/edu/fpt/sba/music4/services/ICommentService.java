package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.request.CommentRequest;
import vn.edu.fpt.sba.music4.dtos.request.UpdateCommentRequest;
import vn.edu.fpt.sba.music4.dtos.response.CommentResponseDTO;
import java.util.List;

public interface ICommentService {
    void addComment(CommentRequest request);
    List<CommentResponseDTO> getCommentsByTrack(Integer trackId);
    void deleteComment(Integer commentId);
    void updateComment(Integer commentId, UpdateCommentRequest request); 
}