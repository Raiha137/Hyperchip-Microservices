package com.hyperchip.user.service;

import com.hyperchip.common.dto.ProductDto;
import com.hyperchip.common.dto.UserDto;
import com.hyperchip.user.model.Address;
import com.hyperchip.user.model.UserDtls;
import com.hyperchip.user.session.SessionUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing users and their addresses in the system.
 *
 * Purpose:
 * - Defines all operations related to user management, including creation, updates, deletion, and retrieval.
 * - Includes address management methods for user shipping/billing addresses.
 * - Supports conversion to session-friendly objects for authentication/session handling.
 */
public interface UserService {

    /**
     * Create a new user.
     *
     * Purpose:
     * - Persists a new UserDtls entity in the database.
     *
     * @param user UserDtls object containing user details
     * @return Persisted UserDtls entity
     */
    UserDtls createUser(UserDtls user);

    /**
     * List users with optional search and pagination.
     *
     * Purpose:
     * - Provides paginated view of users with optional search term.
     *
     * @param search   Optional search string (name/email)
     * @param pageable Pageable object for pagination
     * @return Page of UserDtls
     */
    Page<UserDtls> listUsers(String search, Pageable pageable);

    /**
     * Find a user by their ID.
     *
     * @param id User ID
     * @return Optional containing UserDtls if found
     */
    Optional<UserDtls> findById(Long id);

    /**
     * Find a user by their authentication system ID.
     *
     * @param authUserId Auth system user ID
     * @return Optional containing UserDtls if found
     */
    Optional<UserDtls> findByAuthUserId(Long authUserId);

    /**
     * Check if a user exists by email.
     *
     * @param email Email address
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Update user profile information.
     *
     * @param id     User ID
     * @param update UserDtls containing updated fields
     * @return Updated UserDtls
     */
    UserDtls updateProfile(Long id, UserDtls update);

    /**
     * Soft delete a user (original version using SYSTEM as actor).
     *
     * @param id User ID
     */
    void softDeleteUser(Long id);

    /**
     * Soft delete a user with actor information.
     *
     * @param id          User ID
     * @param performedBy Name or system performing the action
     */
    void softDeleteUser(Long id, String performedBy);

    // --- Address Management ---

    /**
     * Add a new address for a user.
     *
     * @param userId  User ID
     * @param address Address object to persist
     * @return Persisted Address
     */
    Address addAddress(Long userId, Address address);

    /**
     * Update an existing address.
     *
     * @param addressId Address ID
     * @param address   Updated address fields
     * @return Updated Address
     */
    Address updateAddress(Long addressId, Address address);

    /**
     * Soft delete an address (original version).
     *
     * @param addressId Address ID
     */
    void softDeleteAddress(Long addressId);

    /**
     * Soft delete an address with actor information.
     *
     * @param addressId   Address ID
     * @param performedBy Actor performing the deletion
     */
    void softDeleteAddress(Long addressId, String performedBy);

    /**
     * List all addresses for a given user.
     *
     * @param userId User ID
     * @return List of Address objects
     */
    List<Address> listAddresses(Long userId);

    /**
     * Set an address as the default for the user.
     *
     * @param addressId Address ID
     * @param userId    User ID
     * @return Updated Address marked as default
     */
    Address setDefaultAddress(Long addressId, Long userId);

    /**
     * Find an address by its ID.
     *
     * @param id Address ID
     * @return Optional containing Address if found
     */
    Optional<Address> findAddressById(Long id);

    /**
     * Create a user record from an authentication service DTO.
     *
     * @param userDto UserDto from auth service
     * @return Persisted UserDtls
     */
    UserDtls createUserFromAuth(UserDto userDto);

    /**
     * Delete an address completely for a user.
     *
     * @param userId    User ID
     * @param addressId Address ID
     */
    void deleteAddress(Long userId, Long addressId);

    /**
     * Find a user by email.
     *
     * @param email Email address
     * @return Optional containing UserDtls if found
     */
    Optional<UserDtls> findByEmail(String email);

    /**
     * Convert a persisted UserDtls to a session-friendly SessionUser.
     *
     * Purpose:
     * - Adapter method to provide a consistent object for storing in HTTP session.
     *
     * @param user UserDtls entity
     * @return SessionUser object
     */
    SessionUser toSessionUser(UserDtls user);
}
