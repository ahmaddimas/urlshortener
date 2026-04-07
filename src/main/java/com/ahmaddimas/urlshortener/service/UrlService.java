package com.ahmaddimas.urlshortener.service;

import com.ahmaddimas.urlshortener.dto.ShortenRequest;
import com.ahmaddimas.urlshortener.dto.ShortenResponse;
import com.ahmaddimas.urlshortener.model.Url;
import com.ahmaddimas.urlshortener.repository.UrlRepository;
import com.ahmaddimas.urlshortener.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.short-code-length}")
    private int shortCodeLength;

    @Value("${app.max-collision}")
    private int maxCollision;

    @Transactional
    public ShortenResponse shortenUrl(ShortenRequest shortenRequest) {
        String shortCode = generateShortCode();
        Url url = Url.builder()
                .originalUrl(shortenRequest.url())
                .shortCode(shortCode)
                .createdAt(LocalDateTime.now())
                .build();
        urlRepository.save(url);
        log.info("Shortened URL: {} -> {}", shortenRequest.url(), shortCode);
        return new ShortenResponse(buildUrl(shortCode), shortenRequest.url());
    }

    @Transactional(readOnly = true)
    public Url getOriginalUrl(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new NoSuchElementException("URL not found for short code: " + shortCode)
                );
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
