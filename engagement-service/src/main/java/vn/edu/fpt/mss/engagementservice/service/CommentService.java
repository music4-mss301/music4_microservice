package vn.edu.fpt.mss.engagementservice.service;

import vn.edu.fpt.mss.engagementservice.dto.request.CommentRequest;
import vn.edu.fpt.mss.engagementservice.dto.request.CommentUpdateRequest;
import vn.edu.fpt.mss.engagementservice.dto.response.CommentResponse;
import vn.edu.fpt.mss.engagementservice.dto.response.PageResponse;

public interface CommentService {
    PageResponse<CommentResponse> getByTrack(Integer trackId, int page, int size);

    CommentResponse create(CommentRequest request);

    CommentResponse update(Integer id, CommentUpdateRequest request);

    void delete(Integer id);
}
