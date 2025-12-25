package com.hyperchip.auth.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for the auth service.
 *
 * Purpose:
 * - Enable simple in-memory caching for short lived data (e.g. OTPs).
 * - Exposes a CacheManager bean named "otpCache" backed by ConcurrentMap.
 *
 * Developer note (simple):
 * - This is an in-memory cache intended for development / small scale use.
 * - For production, replace with a distributed cache (Redis, Hazelcast, etc.).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Creates a CacheManager with a cache named "otpCache".
     *
     * - Key: cache name ("otpCache")
     * - Implementation: ConcurrentMapCacheManager (simple in-memory map)
     *
     * @return CacheManager instance
     */
    @Bean
    public CacheManager cacheManager() {
        // Creates a simple in-memory cache named "otpCache"
        return new ConcurrentMapCacheManager("otpCache");
    }
}
