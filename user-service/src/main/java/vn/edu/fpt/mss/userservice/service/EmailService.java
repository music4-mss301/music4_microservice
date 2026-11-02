package vn.edu.fpt.mss.userservice.service;

public interface EmailService {
    void sendPasswordReset(String to, String resetLink);
}
