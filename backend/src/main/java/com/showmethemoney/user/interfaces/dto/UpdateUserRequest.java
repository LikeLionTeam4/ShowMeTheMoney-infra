package com.showmethemoney.user.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(@NotBlank String name) {}
