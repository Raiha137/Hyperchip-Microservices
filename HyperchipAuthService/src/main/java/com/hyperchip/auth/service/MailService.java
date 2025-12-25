package com.hyperchip.auth.service;

import com.hyperchip.common.email.EmailService;
import com.hyperchip.common.util.OtpUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * MailService
 * -----------
 * This service is responsible for:
 *  - Sending OTP emails
 *  - Storing OTP in cache with expiry
 *  - Verifying OTP entered by the user
 *
 * Used for:
 *  - Signup email verification
 *  - Forgot / reset password
 */
@Service
public class MailService implements EmailService {

    private static final Logger logger =
            LoggerFactory.getLogger(MailService.class);

    // Sends emails using SMTP configuration
    private final JavaMailSender mailSender;

    // Cache to store OTP values temporarily
    private final Cache otpCache;

    // OTP settings
    private static final int OTP_LENGTH = 4;
    private static final long OTP_EXPIRY_MINUTES = 5;

    // "From" email address (fallback included)
    @Value("${mail.from:${spring.mail.username:no-reply@localhost}}")
    private String fromAddress;

    /**
     * Constructor
     * -----------
     * Ensures mail sender and OTP cache are available
     */
    @Autowired
    public MailService(JavaMailSender mailSender,
                       CacheManager cacheManager) {

        this.mailSender =
                Objects.requireNonNull(mailSender,
                        "JavaMailSender must not be null");

        this.otpCache = cacheManager.getCache("otpCache");

        if (this.otpCache == null) {
            throw new IllegalStateException(
                    "OTP Cache 'otpCache' is not configured");
        }
    }

    // ====================================================
    // PUBLIC METHODS – OTP SENDING
    // ====================================================

    /**
     * Send OTP for new user signup
     */
    public boolean sendSignupOtp(String email) {
        return sendOtpWithTemplate(email, OtpPurpose.SIGNUP);
    }

    /**
     * Send OTP for password reset
     */
    public boolean sendResetPasswordOtp(String email) {
        return sendOtpWithTemplate(email, OtpPurpose.RESET_PASSWORD);
    }

    /**
     * Old generic OTP method
     * (kept for backward compatibility)
     */
    @Deprecated
    public boolean sendOtp(String email) {
        return sendOtpWithTemplate(email, OtpPurpose.GENERIC);
    }

    // ====================================================
    // OTP VERIFICATION
    // ====================================================

    /**
     * Verify OTP entered by the user
     */
    public boolean verifyOtp(String email, String inputOtp) {

        Cache.ValueWrapper wrapper = otpCache.get(email);

        if (wrapper == null) {
            logger.warn("No OTP found for {}", email);
            return false;
        }

        // Stored format: otp|expiryTime
        String storedValue = wrapper.get().toString();
        String[] parts = storedValue.split("\\|");

        if (parts.length != 2) {
            logger.error("Invalid OTP cache format for {}", email);
            return false;
        }

        String storedOtp = parts[0];
        LocalDateTime expiry;

        try {
            expiry = LocalDateTime.parse(parts[1]);
        } catch (Exception e) {
            logger.error("Failed to parse OTP expiry", e);
            return false;
        }

        // Check expiry
        if (LocalDateTime.now().isAfter(expiry)) {
            logger.warn("OTP expired for {}", email);
            otpCache.evict(email);
            return false;
        }

        // Match OTP
        if (storedOtp.equals(inputOtp)) {
            logger.info("OTP verified successfully for {}", email);
            otpCache.evict(email); // OTP used once
            return true;
        }

        logger.info("OTP mismatch for {}", email);
        return false;
    }

    /**
     * Clear OTP manually (if needed)
     */
    public void clearOtp(String email) {
        otpCache.evict(email);
        logger.info("OTP cache cleared for {}", email);
    }

    // ====================================================
    // INTERNAL HELPERS
    // ====================================================

    /**
     * Purpose of OTP
     * Used to change email content
     */
    private enum OtpPurpose {
        SIGNUP,
        RESET_PASSWORD,
        GENERIC
    }

    /**
     * Generate, store and send OTP email
     * based on purpose
     */
    private boolean sendOtpWithTemplate(String email,
                                        OtpPurpose purpose) {

        try {
            // Generate OTP
            String otp = OtpUtil.generateOtp(OTP_LENGTH);

            // Store OTP in cache
            storeOtp(email, otp);

            String subject;
            String body;

            // Choose email content
            switch (purpose) {

                case SIGNUP -> {
                    subject = "Hyperchip – Email verification code";
                    body = """
                            Hi,

                            Use this OTP to verify your email:

                            OTP: %s

                            This code is valid for %d minutes.

                            If you did not sign up, ignore this email.

                            Thanks,
                            Hyperchip Support
                            """.formatted(otp, OTP_EXPIRY_MINUTES);
                }

                case RESET_PASSWORD -> {
                    subject = "Hyperchip – Reset password OTP";
                    body = """
                            Hi,

                            Use this OTP to reset your password:

                            OTP: %s

                            This code is valid for %d minutes.

                            If you did not request this, ignore it.

                            Thanks,
                            Hyperchip Support
                            """.formatted(otp, OTP_EXPIRY_MINUTES);
                }

                default -> {
                    subject = "Your Hyperchip OTP";
                    body = """
                            Your OTP is: %s
                            Valid for %d minutes.

                            Do not share this OTP.
                            """.formatted(otp, OTP_EXPIRY_MINUTES);
                }
            }

            boolean sent = sendEmail(email, subject, body);

            // If mail fails, remove OTP
            if (!sent) {
                otpCache.evict(email);
            }

            return sent;

        } catch (Exception e) {
            logger.error("Error while sending OTP", e);
            return false;
        }
    }

    /**
     * Store OTP with expiry time in cache
     */
    private void storeOtp(String email, String otp) {

        LocalDateTime expiry =
                LocalDateTime.now()
                        .plusMinutes(OTP_EXPIRY_MINUTES);

        String value = otp + "|" + expiry;

        otpCache.put(email, value);

        logger.info("OTP stored for {} until {}", email, expiry);
    }

    // ====================================================
    // EMAIL SENDING (Common Service)
    // ====================================================

    /**
     * Send simple email
     */
    @Override
    public boolean sendEmail(String to,
                             String subject,
                             String body) {

        try {
            SimpleMailMessage message =
                    new SimpleMailMessage();

            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            logger.info("Email sent to {}", to);
            return true;

        } catch (MailException e) {
            logger.error("Email sending failed", e);
            return false;
        }
    }
}
