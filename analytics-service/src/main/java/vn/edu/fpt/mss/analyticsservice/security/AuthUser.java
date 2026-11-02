package vn.edu.fpt.mss.analyticsservice.security;

/** Người dùng hiện tại, được gateway xác thực JWT rồi truyền xuống qua header. */
public record AuthUser(Integer id, String username, String role) {

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isArtist() {
        return "ARTIST".equalsIgnoreCase(role);
    }
}
