package vn.edu.fpt.sba.music4.services.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Async("taskExecutor")
    public void sendResetEmail(String toEmail, String token) {
        try {
            Context context = new Context();
            context.setVariable("resetLink", frontendUrl + "/reset-password?token=" + token);
            context.setVariable("userName", toEmail.split("@")[0]); // Extract username from email

            String html = templateEngine.process("email/reset-password", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Reset Password");
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Reset email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send reset email to: {}", toEmail, e);
            throw new RuntimeException(e);
        }
    }

    @Async("taskExecutor")
    public void sendVerificationEmail(String toEmail, String token) {
        try {
            Context context = new Context();
            context.setVariable("verifyLink", frontendUrl + "/verify-email?token=" + token);
            context.setVariable("userName", toEmail.split("@")[0]);

            String html = templateEngine.process("email/verify-account", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Xác thực tài khoản của bạn");
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Verification email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
        }
    }
}
