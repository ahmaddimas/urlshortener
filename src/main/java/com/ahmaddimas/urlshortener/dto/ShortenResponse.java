package com.ahmaddimas.urlshortener.dto;

public record ShortenResponse(
        String shortUrl,
        String originalUrl
) {}