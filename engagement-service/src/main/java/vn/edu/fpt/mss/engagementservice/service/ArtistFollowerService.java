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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        if (queries == null || queries.isEmpty()) {
            return List.of();
        }
        // 2 câu GROUP BY cho cả danh sách, thay vì 2 câu cho mỗi nghệ sĩ (N+1).
        Map<Integer, Long> followers = toCountMap(followRepository.countFollowersByArtistIds(
                queries.stream().map(ArtistEngagementQuery::artistId).toList()));
        Set<Integer> allTrackIds = queries.stream().filter(q -> q.trackIds() != null)
                .flatMap(q -> q.trackIds().stream()).collect(Collectors.toSet());
        Map<Integer, Long> favorites = allTrackIds.isEmpty()
                ? Map.of() : toCountMap(favoriteRepository.countByTrackIds(allTrackIds));
        return queries.stream().map(q -> new ArtistEngagementResult(q.artistId(),
                followers.getOrDefault(q.artistId(), 0L),
                q.trackIds() == null ? 0L : q.trackIds().stream().distinct()
                        .mapToLong(t -> favorites.getOrDefault(t, 0L)).sum()))
                .toList();
    }

    private static Map<Integer, Long> toCountMap(List<Object[]> rows) {
        Map<Integer, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((Integer) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }
}
