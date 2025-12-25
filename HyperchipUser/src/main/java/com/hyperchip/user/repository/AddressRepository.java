package com.hyperchip.user.repository;

import com.hyperchip.user.model.Address;
import com.hyperchip.user.model.UserDtls;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Address entity.
 * Provides methods to perform CRUD operations and custom queries related to user addresses.
 */
public interface AddressRepository extends JpaRepository<Address, Long> {

    /**
     * Fetch all addresses for a specific user that are not deleted.
     *
     * @param userId ID of the user
     * @return List of active addresses for the user
     * Usage: Display all addresses of a user in profile or checkout.
     */
    List<Address> findByUserIdAndDeletedFalse(Long userId);

    /**
     * Fetch the default address for a specific user that is not deleted.
     *
     * @param userId ID of the user
     * @return Optional containing the default address if present
     * Usage: Automatically select default shipping address during checkout.
     */
    Optional<Address> findByUserIdAndIsDefaultTrueAndDeletedFalse(Long userId);

    /**
     * Fetch a single address by ID that is not deleted.
     *
     * @param id ID of the address
     * @return Optional containing the address if it exists
     * Usage: View or edit a specific address in the profile.
     */
    Optional<Address> findByIdAndDeletedFalse(Long id);

    /**
     * Fetch all addresses associated with a specific UserDtls object.
     *
     * @param user User entity
     * @return List of addresses linked to the user
     * Usage: General-purpose method to retrieve addresses when user object is available.
     */
    List<Address> findByUser(UserDtls user);

    /**
     * Fetch a specific address by ID and ensure it belongs to a specific user.
     *
     * @param id Address ID
     * @param userId User ID
     * @return Optional containing the address if it belongs to the user
     * Usage: Validate ownership of an address before updating or deleting.
     */
    Optional<Address> findByIdAndUserId(Long id, Long userId);

    /**
     * Fetch all non-deleted addresses for a user using underscore property syntax.
     *
     * @param userId ID of the user
     * @return List of active addresses for the user
     * Usage: Alternative query syntax to findByUserIdAndDeletedFalse, useful for consistency.
     */
    List<Address> findByUser_IdAndDeletedFalse(Long userId);
}
