package com.ahmaddimas.urlshortener.service;

import com.ahmaddimas.urlshortener.dto.ShortenRequest;
import com.ahmaddimas.urlshortener.dto.ShortenResponse;
import com.ahmaddimas.urlshortener.model.Url;
import com.ahmaddimas.urlshortener.repository.UrlRepository;
import com.ahmaddimas.urlshortener.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;

    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.short-code-length}")
    private int shortCodeLength;

    @Value("${app.max-collision}")
    private int maxCollision;

    @Transactional
    public ShortenResponse shortenUrl(ShortenRequest shortenRequest) {

        URI uri = URI.create(shortenRequest.url());
        String lockKey = "lock:shorten:host:" + uri.getHost();

        var existing = urlRepository.findByOriginalUrl(shortenRequest.url());
        if (existing.isPresent()) {
            log.info("URL already shortened: {} -> {}", shortenRequest.url(), existing.get().getShortCode());
            return new ShortenResponse(buildUrl(existing.get().getShortCode()), shortenRequest.url());
        }

        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            existing = urlRepository.findByOriginalUrl(shortenRequest.url());
            if (existing.isPresent()) {
                log.info("URL already shortened: {} -> {}", shortenRequest.url(), existing.get().getShortCode());
                return new ShortenResponse(buildUrl(existing.get().getShortCode()), shortenRequest.url());
            }

            String shortCode = generateShortCode();
            Url url = Url.builder()
                    .originalUrl(shortenRequest.url())
                    .shortCode(shortCode)
                    .createdAt(LocalDateTime.now())
                    .build();
            urlRepository.save(url);
            log.info("Shortened URL: {} -> {}", shortenRequest.url(), shortCode);
            return new ShortenResponse(buildUrl(shortCode), shortenRequest.url());
        } finally {
            lock.unlock();
        }
    }

    @Transactional(readOnly = true)
    public Url getOriginalUrl(String shortCode) {
        String cacheKey = "cache:url:" + shortCode;
        String lockKey = "lock:url:" + shortCode;

        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);
        if (cachedUrl != null) {
            return Url.builder()
                    .originalUrl(cachedUrl)
                    .shortCode(shortCode)
                    .build();
        }

        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            cachedUrl = redisTemplate.opsForValue().get(cacheKey);
            if (cachedUrl != null) {
                return Url.builder()
                        .originalUrl(cachedUrl)
                        .shortCode(shortCode)
                        .build();
            }

            Url url = urlRepository.findByShortCode(shortCode)
                    .orElseThrow(() ->
                            new NoSuchElementException("URL not found for short code: " + shortCode)
                    );
            redisTemplate.opsForValue().set(cacheKey, url.getOriginalUrl(), CACHE_TTL);
            return url;
        } finally {
            lock.unlock();
        }
    }

    private String generateShortCode() {
        for (int attempt = 0; attempt < maxCollision; attempt++) {
            String shortCode = base62Encoder.generateCode(shortCodeLength);
            if (!urlRepository.existsByShortCode(shortCode)) {
                return shortCode;
            }
            log.warn("Collision detected for short code: {}, attempt: {}", shortCode, attempt + 1);
        }
        throw new IllegalStateException("Max collision reached for short code generation.");
    }

    private String buildUrl(String shortCode) {
        return baseUrl + "/" + shortCode;
    }
}
