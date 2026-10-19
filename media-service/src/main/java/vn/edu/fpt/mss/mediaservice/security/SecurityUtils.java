package vn.edu.fpt.mss.mediaservice.security;

import vn.edu.fpt.mss.mediaservice.exception.ForbiddenException;
import vn.edu.fpt.mss.mediaservice.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;
import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<AuthUser> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static AuthUser requireUser() {
        return currentUser().orElseThrow(() -> new UnauthorizedException("Bạn cần đăng nhập"));
    }

    public static Integer currentUserIdOrNull() {
        return currentUser().map(AuthUser::id).orElse(null);
    }

    public static boolean isAdmin() {
        return currentUser().map(AuthUser::isAdmin).orElse(false);
    }

    /** Chỉ chủ sở hữu hoặc ADMIN mới được thao tác. */
    public static void requireOwnerOrAdmin(Integer ownerUserId) {
        AuthUser user = requireUser();
        if (!user.isAdmin() && !Objects.equals(user.id(), ownerUserId)) {
            throw new ForbiddenException("Bạn không có quyền với tài nguyên này");
        }
    }
}
