package movieapp.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Async("taskExecutor")
    public void sendResetPasswordEmail(String to, String userName, String token) {
        try {
            String resetUrl = frontendUrl + "/reset-password?token=" + token;
            String htmlTemplate = loadHtmlTemplate("templates/reset-password.html");
            String htmlContent = htmlTemplate.replace("{{RESET_URL}}", resetUrl)
                    .replace("{{USER_NAME}}", userName != null ? userName : "");

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject("Đặt lại mật khẩu - Streamvibe");
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Async("taskExecutor")
    public void sendActiveEmail(String to, String userName, String token) {
        try {
            String verifyUrl = frontendUrl + "/verify-email?token=" + token;
            String htmlTemplate = loadHtmlTemplate("templates/active-email.html");
            String htmlContent = htmlTemplate.replace("{{VERIFY_URL}}", verifyUrl)
                    .replace("{{USER_NAME}}", userName != null ? userName : "");
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject("Kích hoạt tài khoản - Streamvibe");
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Async("taskExecutor")
    public void sendChangeEmail(String to, String userName, String currentEmail, String newEmail, String token) {
        try {
            String verifyUrl = frontendUrl + "/change-email/confirm?token=" + token;
            String htmlTemplate = loadHtmlTemplate("templates/change-email.html");
            String htmlContent = htmlTemplate.replace("{{VERIFY_URL}}", verifyUrl).replace("{{USER_NAME}}", userName != null ? userName : "")
                    .replace("{{CURRENT_EMAIL}}", currentEmail).replace("{{NEW_EMAIL}}", newEmail);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject("Thay đổi email - Streamvibe");
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Async("taskExecutor")
    public void sendWarningEmail(String to, String userName, String currentEmail, String newEmail) {
        try {
            String htmlTemplate = loadHtmlTemplate("templates/warning-change-email.html");
            String htmlContent = htmlTemplate.replace("{{USER_NAME}}", userName != null ? userName : "")
                    .replace("{{CURRENT_EMAIL}}", currentEmail).replace("{{NEW_EMAIL}}", newEmail);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject("Cảnh Báo Thay đổi email - Streamvibe");
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String loadHtmlTemplate(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
