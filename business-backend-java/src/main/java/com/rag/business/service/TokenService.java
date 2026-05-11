package com.rag.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redisTemplate;

    private static final String TOKEN_PREFIX = "token:";
    private static final long TOKEN_EXPIRATION_HOURS = 24;

    public void saveToken(Long userId, String token) {
        String key = TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(key, token, TOKEN_EXPIRATION_HOURS, TimeUnit.HOURS);
        log.info("Token已缓存到Redis, userId={}, expiration={}小时", userId, TOKEN_EXPIRATION_HOURS);
    }

    public boolean validateToken(Long userId, String token) {
        String key = TOKEN_PREFIX + userId;
        String cachedToken = redisTemplate.opsForValue().get(key);

        if (cachedToken == null) {
            log.info("Token不存在或已过期, userId={}", userId);
            return false;
        }

        if (!cachedToken.equals(token)) {
            log.warn("Token不匹配, userId={}, expected={}, actual={}", userId, cachedToken, token);
            return false;
        }

        return true;
    }

    public void removeToken(Long userId) {
        String key = TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("Token已从Redis移除, userId={}", userId);
    }

    public String getTokenFromRedis(Long userId) {
        String key = TOKEN_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }
}