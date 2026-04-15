package com.devices.web.dto;

import com.devices.domain.Device;
import com.devices.domain.DeviceState;

import java.time.Instant;

public record DeviceResponse(Long id, String name, String brand, DeviceState state, Instant createdAt) {

    public static DeviceResponse from(Device d) {
        return new DeviceResponse(d.getId(), d.getName(), d.getBrand(), d.getState(), d.getCreatedAt());
    }
}
