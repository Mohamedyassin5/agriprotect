package tn.esprit.agri.services;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.name:Agri}")
    private String appName;

    @Value("${app.mail.from:no-reply@agri.tn}")
    private String from;

    public void sendResetCode(String to, String name, String code, int expiryMinutes) {
        try {
            Context ctx = new Context();
            ctx.setVariable("name", name);
            ctx.setVariable("code", code);
            ctx.setVariable("expiryMinutes", expiryMinutes);
            ctx.setVariable("appName", appName);

            String html = templateEngine.process("emails/reset-code", ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Code de réinitialisation - " + appName);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
