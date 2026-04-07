package com.ahmaddimas.urlshortener.controller;

import com.ahmaddimas.urlshortener.dto.Response;
import com.ahmaddimas.urlshortener.dto.ShortenRequest;
import com.ahmaddimas.urlshortener.model.Url;
import com.ahmaddimas.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @PostMapping(value = "/api/v1/shorten", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> shortenUrl(@Valid ShortenRequest shortenRequest) {
        log.info("Received shorten request: {}", shortenRequest);

        Response response = Response.builder()
                .message("URL shortened successfully.")
                .data(urlService.shortenUrl(shortenRequest))
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectToOriginalUrl(@PathVariable String shortCode) {
        log.info("Redirecting to original URL for short code: {}", shortCode);

        Url url = urlService.getOriginalUrl(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, url.getOriginalUrl())
                .build();
    }
}
