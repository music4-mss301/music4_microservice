package vn.edu.fpt.sba.music4.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.sba.music4.dtos.response.FollowResponseDTO;
import vn.edu.fpt.sba.music4.entities.Follow;
import vn.edu.fpt.sba.music4.repositories.ArtistRepository;
import vn.edu.fpt.sba.music4.repositories.FollowRepository;
import vn.edu.fpt.sba.music4.repositories.UserRepository;
import vn.edu.fpt.sba.music4.services.IFollowService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements IFollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;

    @Override
    @Transactional
    public boolean toggleFollow(Integer userId, Integer artistId) {
        Follow existingFollow = followRepository.findByUserIdAndArtistId(userId, artistId).orElse(null);

        if (existingFollow != null) {
            followRepository.delete(existingFollow);
            return false; // Đã hủy follow
        } else {
            Follow newFollow = new Follow();
            newFollow.setUser(userRepository.getReferenceById(userId));
            newFollow.setArtist(artistRepository.getReferenceById(artistId));
            newFollow.setFollowedAt(LocalDateTime.now());

            followRepository.save(newFollow);
            return true; // Đã follow thành công
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowResponseDTO> getMyFollowings(Integer userId) {
        List<Follow> follows = followRepository.findByUserId(userId);
        return follows.stream()
                .map(f -> new FollowResponseDTO(
                        f.getId(),
                        f.getArtist().getId(),
                        f.getArtist().getName(),
                        f.getArtist().getImg(),
                        f.getFollowedAt()
                )).toList();
    }
}