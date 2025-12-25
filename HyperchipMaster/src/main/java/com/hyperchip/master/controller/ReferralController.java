package com.hyperchip.master.controller;

import com.hyperchip.common.dto.ReferralCodeDto;
import com.hyperchip.common.dto.ReferralRegistrationRequest;
import com.hyperchip.common.dto.ReferralRewardDto;
import com.hyperchip.master.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ReferralController
 *
 * Purpose:
 * - Manage referral codes and referral rewards in the system.
 * - Allows users to get their referral code, register new users via referral,
 *   and view rewards earned from referrals.
 */
@RestController
@RequestMapping("/api/referrals") // Base path for referral-related APIs
@RequiredArgsConstructor             // Auto-generate constructor for final fields
public class ReferralController {

    private final OfferService offerService; // Service layer handling referral logic

    // ===========================
    // GET OR CREATE REFERRAL CODE
    // ===========================
    /**
     * POST /api/referrals/code/{userId}
     *
     * Purpose:
     * - Existing user requests their referral code.
     * - If the user doesn’t have a code, a new one is generated.
     *
     * @param userId ID of the existing user
     * @return ReferralCodeDto containing the referral code
     */
    @PostMapping("/code/{userId}")
    public ResponseEntity<ReferralCodeDto> getOrCreateCode(@PathVariable Long userId) {
        ReferralCodeDto dto = offerService.createOrGetReferralCode(userId);
        return ResponseEntity.ok(dto);
    }

    // ===========================
    // REGISTER NEW USER WITH REFERRAL
    // ===========================
    /**
     * POST /api/referrals/register
     *
     * Purpose:
     * - Handles registration of a new user using a referral code.
     * - Updates the reward for the referrer if the referral is valid.
     *
     * @param req DTO containing referralCode, newUserId, newUserEmail
     * @return ReferralRewardDto containing reward info for the referrer
     */
    @PostMapping("/register")
    public ResponseEntity<ReferralRewardDto> registerReferral(
            @RequestBody ReferralRegistrationRequest req) {

        ReferralRewardDto dto = offerService.registerReferral(
                req.getReferralCode(), req.getNewUserId(), req.getNewUserEmail());
        return ResponseEntity.ok(dto);
    }

    // ===========================
    // GET REWARDS FOR REFERRER
    // ===========================
    /**
     * GET /api/referrals/rewards/{referrerId}
     *
     * Purpose:
     * - Fetches all referral rewards earned by a specific referrer.
     *
     * @param referrerId ID of the referrer
     * @return List of ReferralRewardDto representing all rewards
     */
    @GetMapping("/rewards/{referrerId}")
    public List<ReferralRewardDto> getRewards(@PathVariable Long referrerId) {
        return offerService.getRewardsForReferrer(referrerId);
    }
}
