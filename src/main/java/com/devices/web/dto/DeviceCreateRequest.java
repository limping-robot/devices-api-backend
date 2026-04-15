package com.devices.web.dto;

import com.devices.domain.DeviceState;
import jakarta.validation.constraints.NotBlank;

public record DeviceCreateRequest(
        @NotBlank String name,
        @NotBlank String brand,
        DeviceState state
) {}
