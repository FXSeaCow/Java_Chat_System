package com.javachat.server;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

final class SmtpMailSender {
    private static final String UTF_8 = StandardCharsets.UTF_8.name();
    private final String username;
    private final String password;
    private final Session session;

    SmtpMailSender(Properties settings) {
        username = settings.getProperty("email.smtp.username", "").strip();
        password = settings.getProperty("email.smtp.app_password", "").replaceAll("\\s+", "");
        Properties smtp = new Properties();
        smtp.setProperty("mail.smtp.host", "smtp.gmail.com");
        smtp.setProperty("mail.smtp.port", "587");
        smtp.setProperty("mail.smtp.auth", "true");
        smtp.setProperty("mail.smtp.auth.mechanisms", "LOGIN PLAIN");
        smtp.setProperty("mail.smtp.starttls.required", "true");
        smtp.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        smtp.setProperty("mail.smtp.connectiontimeout", "5000");
        smtp.setProperty("mail.smtp.timeout", "5000");
        smtp.setProperty("mail.smtp.writetimeout", "5000");
        session = Session.getInstance(smtp);
    }

    void send(String recipient, String subject, String body) throws IOException {
        if (username.isBlank() || password.isBlank()) {
            throw new IOException("SMTP credentials are missing");
        }
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username, "E-Chat", UTF_8));
            InternetAddress to = new InternetAddress(recipient, true);
            to.validate();
            if (to.isGroup() || !recipient.equals(to.getAddress())) {
                throw new IOException("Invalid SMTP recipient");
            }
            message.setRecipient(Message.RecipientType.TO, to);
            message.setSubject(subject, UTF_8);
            message.setText(body, UTF_8);
            Transport.send(message, username, password);
        } catch (MessagingException | UnsupportedEncodingException error) {
            throw new IOException("SMTP delivery failed (" + error.getClass().getSimpleName() + ")", error);
        }
    }
}
