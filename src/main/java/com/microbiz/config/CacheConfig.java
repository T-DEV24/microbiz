package com.microbiz.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("stats_ca_total", "stats_evolution_mensuelle");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(Duration.ofMinutes(10)));
        return manager;
    }
}
