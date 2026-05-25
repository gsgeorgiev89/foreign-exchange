package com.example.fx.dto;

public record ErrorResponse(
    String code,
    String message
) {}