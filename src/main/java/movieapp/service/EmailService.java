package movieapp.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import movieapp.config.properties.MailProperties;
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
    private final MailProperties mailProperties;


    @Async("taskExecutor")
    public void sendResetPasswordEmail(String to, String userName, String token) {
        try {
            String htmlTemplate = loadHtmlTemplate("templates/reset-password.html");
            String htmlContent = htmlTemplate
                    .replace("{{USER_NAME}}", userName != null ? userName : "")
                    .replace("{{TOKEN}}", token);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            helper.setTo(to);
            helper.setFrom(mailProperties.getUsername());
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
            String htmlTemplate = loadHtmlTemplate("templates/active-email.html");
            String htmlContent = htmlTemplate
                    .replace("{{USER_NAME}}", userName != null ? userName : "")
                    .replace("{{TOKEN}}", token);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            helper.setTo(to);
            helper.setFrom(mailProperties.getUsername());
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
            String htmlTemplate = loadHtmlTemplate("templates/change-email.html");
            String htmlContent = htmlTemplate.replace("{{USER_NAME}}", userName != null ? userName : "")
                    .replace("{{CURRENT_EMAIL}}", currentEmail).replace("{{NEW_EMAIL}}", newEmail)
                    .replace("{{TOKEN}}", token);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            helper.setTo(to);
            helper.setFrom(mailProperties.getUsername());
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
            helper.setFrom(mailProperties.getUsername());
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
