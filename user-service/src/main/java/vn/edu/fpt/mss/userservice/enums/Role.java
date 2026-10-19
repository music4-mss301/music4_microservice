package vn.edu.fpt.mss.userservice.enums;

public enum Role {
    LISTENER, ARTIST, ADMIN;

    public static Role from(String value) {
        if (value == null || value.isBlank()) {
            return LISTENER;
        }
        String v = value.trim().toUpperCase().replace("ROLE_", "");
        return "USER".equals(v) ? LISTENER : Role.valueOf(v);
    }
}
