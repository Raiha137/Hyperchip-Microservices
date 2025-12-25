package com.hyperchip.user.service;

import com.hyperchip.common.dto.ContactDto;

/**
 * Service interface for handling user contact messages.
 *
 * Purpose:
 * - Defines the contract for operations related to contact messages submitted by users.
 * - Promotes loose coupling between controllers and the actual service implementation.
 */
public interface ContactService {

    /**
     * Save a contact message submitted by a user.
     *
     * Purpose:
     * - Accepts a ContactDto containing user message details and persists it.
     * - Implementation may store the message in the database for later review or follow-up.
     *
     * @param contactDto Data transfer object containing message details (name, email, subject, message, etc.)
     */
    void saveContact(ContactDto contactDto);
}
