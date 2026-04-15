package com.devices.service;

import com.devices.domain.DeviceState;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Parsed PATCH body: only {@code name}, {@code brand}, and {@code state} are accepted.
 * {@code createdAt} is rejected; other unknown properties are ignored.
 */
public record DevicePatchPayload(Optional<String> name, Optional<String> brand, Optional<DeviceState> state) {

    public static DevicePatchPayload from(JsonNode body) {
        if (body == null || body.isNull() || !body.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON object body required");
        }
        if (body.has("createdAt")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field createdAt cannot be updated");
        }
        Optional<String> name = Optional.empty();
        if (body.has("name")) {
            if (body.get("name").isNull()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name cannot be null");
            }
            name = Optional.of(body.get("name").asText());
        }
        Optional<String> brand = Optional.empty();
        if (body.has("brand")) {
            if (body.get("brand").isNull()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "brand cannot be null");
            }
            brand = Optional.of(body.get("brand").asText());
        }
        Optional<DeviceState> state = Optional.empty();
        if (body.has("state")) {
            if (body.get("state").isNull()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "state cannot be null");
            }
            var raw = body.get("state").asText();
            try {
                state = Optional.of(DeviceState.valueOf(raw));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid state: " + raw);
            }
        }
        return new DevicePatchPayload(name, brand, state);
    }
}
