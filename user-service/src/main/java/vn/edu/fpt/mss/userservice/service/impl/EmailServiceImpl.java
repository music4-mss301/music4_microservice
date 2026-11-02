package vn.edu.fpt.mss.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import vn.edu.fpt.mss.userservice.service.EmailService;

/**
 * Gửi mail qua SMTP. Lưu ý: Render free chặn cổng SMTP 25/465/587 -> trên production cần thay
 * implementation này bằng API HTTP (Resend/Brevo). Khi chưa cấu hình mail, link được ghi ra log để demo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    @Async
    @Override
    public void sendPasswordReset(String to, String resetLink) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || from.isBlank()) {
            log.warn("Mail chưa cấu hình. Link đặt lại mật khẩu cho {}: {}", to, resetLink);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject("[Music4] Đặt lại mật khẩu");
            msg.setText("Bấm vào link sau để đặt lại mật khẩu (hết hạn sau 30 phút):\n" + resetLink);
            sender.send(msg);
        } catch (Exception ex) {
            log.error("Gửi mail thất bại tới {}: {}", to, ex.getMessage());
        }
    }
}
