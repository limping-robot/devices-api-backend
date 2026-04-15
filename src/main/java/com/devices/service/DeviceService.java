package com.devices.service;

import com.devices.domain.Device;
import com.devices.domain.DeviceState;
import com.devices.repo.DeviceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DeviceService {

    private final DeviceRepository devices;

    public DeviceService(DeviceRepository devices) {
        this.devices = devices;
    }

    @Transactional
    public Device create(String name, String brand, DeviceState state) {
        var d = new Device();
        d.setName(name);
        d.setBrand(brand);
        d.setState(state != null ? state : DeviceState.AVAILABLE);
        return devices.save(d);
    }

    @Transactional(readOnly = true)
    public Device get(Long id) {
        return devices.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Device> list(String brand, DeviceState state) {
        boolean hasBrand = brand != null && !brand.isBlank();
        if (hasBrand && state != null) {
            return devices.findByBrandIgnoreCaseAndState(brand, state);
        }
        if (hasBrand) {
            return devices.findByBrandIgnoreCase(brand);
        }
        if (state != null) {
            return devices.findByState(state);
        }
        return devices.findAll();
    }

    @Transactional
    public Device replace(Long id, String name, String brand, DeviceState state) {
        var d = get(id);
        DeviceRules.validateInUseIdentityLocks(d.getName(), d.getBrand(), name, brand, state);
        d.setName(name);
        d.setBrand(brand);
        d.setState(state);
        return devices.save(d);
    }

    @Transactional
    public Device patch(Long id, DevicePatchPayload patch) {
        var d = get(id);
        String resultingName = patch.name().orElse(d.getName());
        String resultingBrand = patch.brand().orElse(d.getBrand());
        DeviceState resultingState = patch.state().orElse(d.getState());

        DeviceRules.validateInUseIdentityLocks(d.getName(), d.getBrand(), resultingName, resultingBrand, resultingState);

        patch.name().ifPresent(d::setName);
        patch.brand().ifPresent(d::setBrand);
        patch.state().ifPresent(d::setState);
        return devices.save(d);
    }

    @Transactional
    public void delete(Long id) {
        var d = get(id);
        if (d.getState() == DeviceState.IN_USE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete device in use");
        }
        devices.deleteById(id);
    }
}
