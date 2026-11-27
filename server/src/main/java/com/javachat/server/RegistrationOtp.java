package com.javachat.server;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class RegistrationOtp {
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Challenge> challenges = new HashMap<>();
    private final SmtpMailSender mail;

    RegistrationOtp(SmtpMailSender mail) {
        this.mail = mail;
    }

    void send(String address, String subject) throws IOException {
        String email = normalize(address);
        Challenge pending;
        synchronized (challenges) {
            Instant now = Instant.now();
            challenges.values().removeIf(challenge -> !now.isBefore(challenge.expires));
            Challenge previous = challenges.get(email);
            if (previous != null && now.isBefore(previous.resendAt) || challenges.size() >= 1000)
                throw new Rejected(429, "Vui lòng chờ trước khi gửi lại mã OTP.");
            String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
            pending = new Challenge(code, now);
            challenges.put(email, pending);
        }
        boolean sent = false;
        try {
            sendEmail(email, subject, "Mã OTP của bạn là " + pending.code + ". Mã hết hạn sau 5 phút.");
            sent = true;
        } finally {
            synchronized (challenges) {
                if (sent) pending.delivered = true;
                else challenges.remove(email, pending);
            }
        }
    }

    void sendEmail(String email, String subject, String message) throws IOException {
        mail.send(normalize(email), subject, message);
    }

    void verify(String address, String code) {
        String email = normalize(address);
        synchronized (challenges) {
            Challenge challenge = challenges.get(email);
            if (challenge == null || !challenge.delivered || !Instant.now().isBefore(challenge.expires)
                    || challenge.attempts == 0)
                throw new Rejected(410, "Mã OTP hết hạn hoặc đã hết lượt thử. Vui lòng yêu cầu mã mới.");
            if (!challenge.code.equals(code)) {
                challenge.attempts--;
                throw new Rejected(422, "Mã OTP không đúng.");
            }
            challenge.attempts = 0;
        }
    }

    private static String normalize(String address) {
        String email = address.strip().toLowerCase(Locale.ROOT);
        if (email.length() > 255 || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("Vui lòng nhập email hợp lệ.");
        return email;
    }

    private static final class Challenge {
        final String code;
        final Instant expires;
        final Instant resendAt;
        int attempts = 5;
        boolean delivered;

        Challenge(String code, Instant now) {
            this.code = code;
            expires = now.plusSeconds(300);
            resendAt = now.plusSeconds(60);
        }
    }

    static final class Rejected extends RuntimeException {
        final int status;

        Rejected(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}
