package com.hyperchip.master.repository;

import com.hyperchip.master.model.ReferralCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for managing ReferralCode entities.
 * Provides methods to fetch referral codes by user or active code.
 */
public interface ReferralCodeRepository extends JpaRepository<ReferralCode, Long> {

    /**
     * Find the referral code associated with a specific user.
     *
     * @param userId the ID of the user
     * @return an Optional containing the ReferralCode if found
     */
    Optional<ReferralCode> findByUserId(Long userId);

    /**
     * Find an active referral code by its code string.
     *
     * @param code the referral code
     * @return an Optional containing the active ReferralCode if found
     */
    Optional<ReferralCode> findByCodeAndActiveTrue(String code);
}
