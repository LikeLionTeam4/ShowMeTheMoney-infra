package com.showmethemoney.auth.interfaces.dto;

public record LoginResponse(String accessToken, String tokenType, Long expiresIn) {}
