package com.hyperchip.user.service.impl;

import com.hyperchip.common.dto.ContactDto;
import com.hyperchip.common.email.EmailService;           // <-- use the common interface
import com.hyperchip.user.model.Contact;
import com.hyperchip.user.repository.ContactRepository;
import com.hyperchip.user.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Implementation of ContactService.
 * Saves contact messages and notifies support via email.
 */
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactServiceImpl.class);

    private final ContactRepository contactRepository;
    private final EmailService mailService; // inject the interface implemented by MailService

    @Override
    public void saveContact(ContactDto contactDto) {

        // Build Contact entity (use Instant for createdAt)
        Contact contact = Contact.builder()
                .name(contactDto.getName())
                .email(contactDto.getEmail())
                .phone(contactDto.getPhone())
                .orderId(contactDto.getOrderId())
                .subject(contactDto.getSubject())
                .message(contactDto.getMessage())
                .urgent(contactDto.getSubject() != null &&
                        contactDto.getSubject().toLowerCase().contains("urgent"))
                .createdAt(Instant.now()) // must be Instant, not LocalDateTime
                .build();

        // Persist
        Contact saved = contactRepository.save(contact);
        log.info("Saved contact id={} email={}", saved.getId(), saved.getEmail());

        // Build email body
        String emailBody = """
                New contact message received:

                Name: %s
                Email: %s
                Phone: %s
                Order ID: %s
                Subject: %s
                Message:
                %s

                Received at: %s
                """.formatted(
                saved.getName(),
                saved.getEmail(),
                saved.getPhone() != null ? saved.getPhone() : "N/A",
                saved.getOrderId() != null ? saved.getOrderId() : "N/A",
                saved.getSubject() != null ? saved.getSubject() : "(no subject)",
                saved.getMessage() != null ? saved.getMessage() : "(no message)",
                saved.getCreatedAt() // Instant.toString() is fine
        );

        // Send email. MailService returns boolean; protect send failure with try/catch
        try {
            boolean sent = mailService.sendEmail(
                    "hyperchip.team@gmail.com",
                    "New Contact Message: " + (saved.getSubject() != null ? saved.getSubject() : "Contact"),
                    emailBody
            );
            if (!sent) {
                log.warn("Contact email not sent for contact id={}", saved.getId());
            } else {
                log.info("Contact email sent for contact id={}", saved.getId());
            }
        } catch (Exception e) {
            // Do not fail the flow if email fails; just log it
            log.error("Failed to send contact email for id={}", saved.getId(), e);
        }
    }
}
