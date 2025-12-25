package com.hyperchip.user.service.impl;

import com.hyperchip.common.dto.UserDto;
import com.hyperchip.user.model.Address;
import com.hyperchip.user.model.UserDtls;
import com.hyperchip.user.repository.AddressRepository;
import com.hyperchip.user.repository.UserRepository;
import com.hyperchip.user.service.AuditService;
import com.hyperchip.user.service.UserService;
import com.hyperchip.user.session.SessionUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of UserService.
 * Handles all business logic related to Users and their Addresses,
 * including creation, updating, soft-deletion, and listing.
 * Also integrates with AuditService to log important actions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final AuditService auditService; // logs actions like soft-delete, update, create

    // ---------- User methods ----------

    /**
     * Create a new user with sensible defaults.
     * @param user UserDtls entity to save
     * @return saved UserDtls
     */
    @Override
    public UserDtls createUser(UserDtls user) {
        if (user.getName() == null) user.setName(user.getEmail());
        return userRepository.save(user);
    }

    /**
     * List users with optional search and pagination.
     * Supports sorting by createdAt descending if no sort provided.
     */
    @Override
    public Page<UserDtls> listUsers(String search, Pageable pageable) {
        log.debug("Listing users search='{}' page={} size={}", search, pageable.getPageNumber(), pageable.getPageSize());

        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by("createdAt").descending());
        }

        Page<UserDtls> page;
        if (StringUtils.hasText(search)) {
            log.debug("Applying search filter '{}'", search);
            page = userRepository.findByDeletedFalseAndNameContainingIgnoreCaseOrDeletedFalseAndEmailContainingIgnoreCase(
                    search, search, pageable);
        } else {
            page = userRepository.findByDeletedFalse(pageable);
        }

        log.debug("Found {} users (totalElements={})", page.getContent().size(), page.getTotalElements());
        return page;
    }

    /**
     * Find user by internal ID, excluding soft-deleted users.
     */
    @Override
    public Optional<UserDtls> findById(Long id) {
        log.debug("Find user by id={}", id);
        return userRepository.findById(id).filter(u -> !Boolean.TRUE.equals(u.getDeleted()));
    }

    /**
     * Find user by auth-service ID, excluding soft-deleted users.
     */
    @Override
    public Optional<UserDtls> findByAuthUserId(Long authUserId) {
        log.debug("Find user by authUserId={}", authUserId);
        return userRepository.findByAuthUserId(authUserId)
                .filter(u -> !Boolean.TRUE.equals(u.getDeleted()));
    }

    /**
     * Check if an email already exists.
     */
    @Override
    public boolean existsByEmail(String email) {
        log.debug("Check existsByEmail={}", email);
        return userRepository.existsByEmail(email);
    }

    /**
     * Update profile fields for a user.
     * Logs the change using AuditService.
     */
    @Override
    @Transactional
    public UserDtls updateProfile(Long id, UserDtls update) {
        log.info("Updating profile for user id={}", id);
        UserDtls user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        if (Boolean.TRUE.equals(user.getDeleted())) {
            throw new RuntimeException("User is deleted: " + id);
        }

        if (StringUtils.hasText(update.getName())) user.setName(update.getName());
        if (StringUtils.hasText(update.getEmail())) user.setEmail(update.getEmail());
        if (StringUtils.hasText(update.getMobileNumber())) user.setMobileNumber(update.getMobileNumber());
        if (StringUtils.hasText(update.getProfileImage())) user.setProfileImage(update.getProfileImage());

        UserDtls saved = userRepository.save(user);
        log.info("Profile updated for user id={}", saved.getId());

        auditService.log("User", saved.getId(), "UPDATE_PROFILE", "SYSTEM", "Profile updated");
        return saved;
    }

    /**
     * Soft-delete user by marking deleted flag.
     * Uses SYSTEM as default actor.
     */
    @Override
    @Transactional
    public void softDeleteUser(Long id) {
        softDeleteUser(id, "SYSTEM");
    }

    /**
     * Soft-delete user and log who performed it.
     */
    @Override
    @Transactional
    public void softDeleteUser(Long id, String performedBy) {
        log.info("Soft-deleting user id={} by={}", id, performedBy);
        UserDtls user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        user.setDeleted(true);
        userRepository.save(user);
        auditService.log("User", id, "SOFT_DELETE", performedBy, "soft-deleted via admin");
    }

    // ---------- Address methods ----------

    /**
     * Add a new address for a user.
     * If address marked default, clears previous default addresses.
     */
    @Override
    @Transactional
    public Address addAddress(Long userId, Address address) {
        log.info("Add address for userId={}", userId);
        UserDtls user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        if (Boolean.TRUE.equals(user.getDeleted())) {
            throw new RuntimeException("User is deleted: " + userId);
        }

        address.setUser(user);

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            List<Address> existing = addressRepository.findByUser(user);
            for (Address a : existing) {
                if (Boolean.TRUE.equals(a.getIsDefault())) {
                    a.setIsDefault(false);
                    addressRepository.save(a);
                }
            }
        }

        Address saved = addressRepository.save(address);
        auditService.log("Address", saved.getId(), "CREATE", "SYSTEM", "Address created for user " + userId);
        return saved;
    }

    /**
     * Update address fields.
     * Ensures only one default address per user.
     */
    @Override
    @Transactional
    public Address updateAddress(Long addressId, Address address) {
        log.info("Update address id={}", addressId);
        Address existing = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found: " + addressId));

        if (Boolean.TRUE.equals(existing.getDeleted())) {
            throw new RuntimeException("Address deleted: " + addressId);
        }

        // update all editable fields
        if (StringUtils.hasText(address.getLabel())) existing.setLabel(address.getLabel());
        if (StringUtils.hasText(address.getAddressLine1())) existing.setAddressLine1(address.getAddressLine1());
        if (StringUtils.hasText(address.getAddressLine2())) existing.setAddressLine2(address.getAddressLine2());
        if (StringUtils.hasText(address.getCity())) existing.setCity(address.getCity());
        if (StringUtils.hasText(address.getState())) existing.setState(address.getState());
        if (StringUtils.hasText(address.getPincode())) existing.setPincode(address.getPincode());
        if (StringUtils.hasText(address.getContactName())) existing.setContactName(address.getContactName());
        if (StringUtils.hasText(address.getContactPhone())) existing.setContactPhone(address.getContactPhone());

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            UserDtls user = existing.getUser();
            List<Address> existingAddresses = addressRepository.findByUser(user);
            for (Address a : existingAddresses) {
                if (Boolean.TRUE.equals(a.getIsDefault()) && !a.getId().equals(existing.getId())) {
                    a.setIsDefault(false);
                    addressRepository.save(a);
                }
            }
            existing.setIsDefault(true);
        }

        Address saved = addressRepository.save(existing);
        auditService.log("Address", saved.getId(), "UPDATE", "SYSTEM", "Address updated");
        return saved;
    }

    /**
     * Soft-delete address (default SYSTEM actor).
     */
    @Override
    @Transactional
    public void softDeleteAddress(Long addressId) {
        softDeleteAddress(addressId, "SYSTEM");
    }

    /**
     * Soft-delete address and record performer.
     */
    @Override
    @Transactional
    public void softDeleteAddress(Long addressId, String performedBy) {
        log.info("Soft-delete address id={} by={}", addressId, performedBy);
        Address existing = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found: " + addressId));
        existing.setDeleted(true);
        addressRepository.save(existing);
        auditService.log("Address", addressId, "SOFT_DELETE", performedBy, "address soft-deleted");
    }

    /**
     * List all non-deleted addresses of a user.
     */
    @Override
    public List<Address> listAddresses(Long userId) {
        log.debug("List addresses for userId={}", userId);
        return addressRepository.findByUserIdAndDeletedFalse(userId);
    }

    /**
     * Set a given address as default for the user.
     * Clears previous default.
     */
    @Override
    @Transactional
    public Address setDefaultAddress(Long addressId, Long userId) {
        log.info("Set default address id={} for userId={}", addressId, userId);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found: " + addressId));

        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Address does not belong to user");
        }

        List<Address> userAddresses = addressRepository.findByUser(address.getUser());
        for (Address a : userAddresses) {
            if (Boolean.TRUE.equals(a.getIsDefault())) {
                a.setIsDefault(false);
                addressRepository.save(a);
            }
        }

        address.setIsDefault(true);
        Address saved = addressRepository.save(address);
        auditService.log("Address", saved.getId(), "SET_DEFAULT", "SYSTEM", "Set default address");
        return saved;
    }

    /**
     * Create or update user from an auth-service DTO (e.g., Google login).
     */
    @Override
    @Transactional
    public UserDtls createUserFromAuth(UserDto userDto) {
        Optional<UserDtls> existing = userRepository.findByAuthUserId(userDto.getId());
        if (existing.isPresent()) {
            UserDtls u = existing.get();
            u.setName(userDto.getFullName());
            u.setEmail(userDto.getEmail());
            u.setMobileNumber(userDto.getPhone());
            return userRepository.save(u);
        }

        UserDtls user = new UserDtls();
        user.setAuthUserId(userDto.getId());
        user.setName(userDto.getFullName());
        user.setEmail(userDto.getEmail());
        user.setMobileNumber(userDto.getPhone());
        return userRepository.save(user);
    }

    @Override
    public Optional<Address> findAddressById(Long id) {
        return addressRepository.findById(id);
    }

    /**
     * Delete (soft-delete) an address belonging to a user.
     */
    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        if (userId == null || addressId == null) throw new IllegalArgumentException("userId and addressId required");

        Optional<Address> opt = addressRepository.findByIdAndUserId(addressId, userId);
        if (opt.isEmpty()) {
            throw new IllegalStateException("Address not found or doesn't belong to user");
        }

        Address a = opt.get();
        if (a.getDeleted() != null && a.getDeleted()) return;

        a.setDeleted(true);
        addressRepository.save(a);
    }

    @Override
    public Optional<UserDtls> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public SessionUser toSessionUser(UserDtls user) {
        if (user == null) return null;
        return new SessionUser(user.getId(), user.getName(), user.getEmail(), user.getProfileImage());
    }
}
