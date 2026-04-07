package com.ahmaddimas.urlshortener.dto;

import lombok.Builder;

@Builder
public record Response(String message, Object data) {
}
