package com.hyperchip.master.repository;

import com.hyperchip.master.model.ReferralReward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for managing ReferralReward entities.
 * Provides methods to fetch rewards given to referrers.
 */
public interface ReferralRewardRepository extends JpaRepository<ReferralReward, Long> {

    /**
     * Find all referral rewards associated with a specific referrer.
     *
     * @param referrerUserId the ID of the user who referred others
     * @return a list of ReferralReward entities for the given referrer
     */
    List<ReferralReward> findByReferrerUserId(Long referrerUserId);
}
