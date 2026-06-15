package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.response.FollowResponseDTO;
import java.util.List;

public interface IFollowService {
    boolean toggleFollow(Integer userId, Integer artistId);
    List<FollowResponseDTO> getMyFollowings(Integer userId);
}