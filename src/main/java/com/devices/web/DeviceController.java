package com.devices.web;

import com.devices.domain.DeviceState;
import com.devices.service.DevicePatchPayload;
import com.devices.service.DeviceService;
import com.devices.web.dto.DeviceCreateRequest;
import com.devices.web.dto.DeviceReplaceRequest;
import com.devices.web.dto.DeviceResponse;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@Tag(name = "Devices", description = "Create, read, update, and delete devices")
public class DeviceController {

    private final DeviceService devices;

    public DeviceController(DeviceService devices) {
        this.devices = devices;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a device")
    public DeviceResponse create(@Valid @RequestBody DeviceCreateRequest request) {
        return DeviceResponse.from(devices.create(request.name(), request.brand(), request.state()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one device by id")
    public DeviceResponse get(@PathVariable Long id) {
        return DeviceResponse.from(devices.get(id));
    }

    @GetMapping
    @Operation(summary = "List devices (optionally filter by brand and/or state)")
    public List<DeviceResponse> list(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) DeviceState state) {
        return devices.list(brand, state).stream().map(DeviceResponse::from).toList();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace device fields (full update)")
    public DeviceResponse replace(@PathVariable Long id, @Valid @RequestBody DeviceReplaceRequest request) {
        return DeviceResponse.from(devices.replace(id, request.name(), request.brand(), request.state()));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Partial update",
            description = "Send a subset of name, brand, state. Field createdAt is rejected with 400; other unknown keys are ignored.")
    public DeviceResponse patch(@PathVariable Long id, @RequestBody JsonNode body) {
        return DeviceResponse.from(devices.patch(id, DevicePatchPayload.from(body)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a device (not allowed when in use)")
    public void delete(@PathVariable Long id) {
        devices.delete(id);
    }
}
