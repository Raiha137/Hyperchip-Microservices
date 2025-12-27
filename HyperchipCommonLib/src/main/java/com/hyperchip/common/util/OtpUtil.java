package com.hyperchip.common.util;

import java.util.Random;

public class OtpUtil {
    /**
     * Generate a numeric OTP of the given length (e.g., length=4 produces 4-digit OTP).
     * @param length number of digits
     * @return generated OTP as String
     */
    public static String generateOtp(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("OTP length must be positive");
        }
        int lower = (int) Math.pow(10, length - 1);
        int upper = (int) Math.pow(10, length) - lower;
        int otp = lower + new Random().nextInt(upper);
        return String.valueOf(otp);
    }
}