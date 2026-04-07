package com.ahmaddimas.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShortenRequest(

        @NotBlank(message = "URL must not be blank")
        @Pattern(
                regexp = "^https?://.*",
                message = "URL must start with http:// or https://"
        )
        @Size(max = 2048, message = "URL must not exceed 2048 characters")
        String url
) {}