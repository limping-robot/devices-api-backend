package com.devices.service;

import com.devices.domain.DeviceState;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Domain rules that are not tied to persistence.
 */
public final class DeviceRules {

    private DeviceRules() {}

    /**
     * After an update, if the device would be {@link DeviceState#IN_USE}, name and brand must stay
     * the same as before the update (covers single-request transitions into IN_USE with a rename).
     */
    public static void validateInUseIdentityLocks(
            String originalName,
            String originalBrand,
            String resultingName,
            String resultingBrand,
            DeviceState resultingState) {
        if (resultingState != DeviceState.IN_USE) {
            return;
        }
        if (!resultingName.equals(originalName)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Cannot put device in use with a different name");
        }
        if (!resultingBrand.equals(originalBrand)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Cannot put device in use with a different brand");
        }
    }
}
