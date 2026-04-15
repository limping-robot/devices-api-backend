package com.devices.web.dto;

import com.devices.domain.DeviceState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceReplaceRequest(
        @NotBlank String name,
        @NotBlank String brand,
        @NotNull DeviceState state
) {}
