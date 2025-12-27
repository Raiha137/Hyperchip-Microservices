package com.hyperchip.common.email;

public interface EmailService {
    /**
     * Send an email with the given recipient, subject, and body.
     * @param to      recipient email address
     * @param subject email subject
     * @param body    email body text
     * @return true if email was sent successfully
     */
    boolean sendEmail(String to, String subject, String body);
}
