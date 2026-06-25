package vn.edu.fpt.sba.music4.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.sba.music4.dtos.response.FavoriteResponseDTO;
import vn.edu.fpt.sba.music4.entities.Favorite;
import vn.edu.fpt.sba.music4.entities.Track;
import vn.edu.fpt.sba.music4.entities.User;
import vn.edu.fpt.sba.music4.repositories.FavoriteRepository;
import vn.edu.fpt.sba.music4.repositories.TrackRepository;
import vn.edu.fpt.sba.music4.repositories.UserRepository;
import vn.edu.fpt.sba.music4.services.IFavoriteService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements IFavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final TrackRepository trackRepository;

    private User getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản đăng nhập."));
    }

    @Override
    @Transactional
    public boolean toggleFavorite(Integer trackId) {
        User currentUser = getAuthenticatedUser();
        Favorite favorite = favoriteRepository.findByUserIdAndTrackId(currentUser.getId(), trackId).orElse(null);

        if (favorite != null) {
            favoriteRepository.delete(favorite);
            return false;
        } else {
            Track track = trackRepository.findById(trackId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bài hát yêu cầu."));

            Favorite newFavorite = new Favorite();
            newFavorite.setUser(currentUser);
            newFavorite.setTrack(track);
            newFavorite.setLikedAt(LocalDateTime.now());

            favoriteRepository.save(newFavorite);
            return true;
        }
    }
    @Override
    @Transactional(readOnly = true)
    public List<FavoriteResponseDTO> getMyFavorites() {
        User currentUser = getAuthenticatedUser();

        List<Favorite> favorites = favoriteRepository.findByUserId(currentUser.getId());
        return favorites.stream()
                .map(f -> new FavoriteResponseDTO(
                        f.getId(),
                        f.getTrack().getId(),
                        f.getTrack().getName(),
                        f.getTrack().getImg(),
                        f.getLikedAt()
                ))
                .toList();
    }

}