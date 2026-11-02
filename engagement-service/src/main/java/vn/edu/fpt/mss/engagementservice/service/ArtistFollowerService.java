package vn.edu.fpt.mss.engagementservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.mss.engagementservice.client.UserClient;
import vn.edu.fpt.mss.engagementservice.dto.client.UserSummary;
import vn.edu.fpt.mss.engagementservice.dto.request.ArtistEngagementQuery;
import vn.edu.fpt.mss.engagementservice.dto.response.ArtistEngagementResult;
import vn.edu.fpt.mss.engagementservice.repository.FavoriteRepository;
import vn.edu.fpt.mss.engagementservice.repository.FollowRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistFollowerService {

    private final FollowRepository followRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserClient userClient;

    /** Danh sách người theo dõi một nghệ sĩ: id ở engagement, tên và ảnh lấy từ user-service (1 lần gọi batch). */
    public List<UserSummary> followersOf(Integer artistId) {
        List<Integer> userIds = followRepository.findFollowerIds(List.of(artistId));
        return userIds.isEmpty() ? List.of() : userClient.getUsers(userIds);
    }

    /** Dashboard admin: followers + favorites của nhiều nghệ sĩ trong một lần gọi từ content-service. */
    public List<ArtistEngagementResult> engagementOf(List<ArtistEngagementQuery> queries) {
        return queries.stream().map(q -> new ArtistEngagementResult(q.artistId(),
                followRepository.countByArtistId(q.artistId()),
                q.trackIds() == null || q.trackIds().isEmpty() ? 0 : favoriteRepository.countByTrackIdIn(q.trackIds())))
                .toList();
    }
}
