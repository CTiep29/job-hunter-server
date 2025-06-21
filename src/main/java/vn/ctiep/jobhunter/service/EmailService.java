package vn.ctiep.jobhunter.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import vn.ctiep.jobhunter.domain.Job;
import vn.ctiep.jobhunter.repository.JobRepository;

@Service
public class EmailService {

    private final MailSender mailSender;
    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;
    private final JobRepository jobRepository;

    public EmailService(MailSender mailSender,
            JavaMailSender javaMailSender,
            SpringTemplateEngine templateEngine,
            JobRepository jobRepository) {
        this.mailSender = mailSender;
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
        this.jobRepository = jobRepository;
    }

    public void sendSimpleEmail() {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo("ads.nctpulga29@gmail.com");
        msg.setSubject("Testing from Spring Boot");
        msg.setText("Hello World from Spring Boot Email");
        this.mailSender.send(msg);
    }

    public void sendEmailSync(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        // Prepare message using a Spring helper
        MimeMessage mimeMessage = this.javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content, isHtml);
            this.javaMailSender.send(mimeMessage);
        } catch (MailException | MessagingException e) {
            System.out.println("ERROR SEND EMAIL: " + e);
        }
    }

    @Async
    public void sendEmailFromTemplateSync(
            String to,
            String subject,
            String templateName,
            String username,
            Object value) {

        Context context = new Context();
        context.setVariable("name", username);
        context.setVariable("jobs", value);

        String content = templateEngine.process(templateName, context);
        this.sendEmailSync(to, subject, content, false, true);
    }
    public void sendInterviewInvitationEmail(String to, String name, String jobTitle, String companyName, String confirmationUrl) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("jobTitle", jobTitle);
        context.setVariable("companyName", companyName);
        context.setVariable("confirmationUrl", confirmationUrl);

        String content = templateEngine.process("interview-invitation", context);
        sendEmailSync(to, "Thư mời phỏng vấn", content, false, true);
    }
    public void sendInterviewPassedEmail(String to, String name, String jobTitle, String companyName, String confirmationUrl) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("jobTitle", jobTitle);
        context.setVariable("companyName", companyName);
        context.setVariable("confirmationUrl", confirmationUrl);

        String content = templateEngine.process("interview-passed", context);
        sendEmailSync(to, "Kết quả phỏng vấn", content, false, true);
    }

    public void sendRejectionEmail(String to, String name, String jobTitle, String companyName) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("jobTitle", jobTitle);
        context.setVariable("companyName", companyName);

        String content = templateEngine.process("rejected", context);
        sendEmailSync(to, "Kết quả ứng tuyển", content, false, true);
    }

    public void sendHiredEmail(String to, String name, String jobTitle, String companyName) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("jobTitle", jobTitle);
        context.setVariable("companyName", companyName);

        String content = templateEngine.process("hired", context);
        sendEmailSync(to, "Chúc mừng bạn đã được tuyển dụng", content, false, true);
    }

    public void sendInterviewFailedEmail(String to, String name, String jobTitle, String companyName) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("jobTitle", jobTitle);
        context.setVariable("companyName", companyName);

        String content = templateEngine.process("interview-failed", context);
        sendEmailSync(to, "Kết quả phỏng vấn", content, false, true);
    }

}
