package com.devices.service;

import com.devices.domain.DeviceState;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceRulesTest {

    @Test
    void whenResultingStateIsInUse_nameAndBrandMustMatchOriginal() {
        assertThatThrownBy(() -> DeviceRules.validateInUseIdentityLocks(
                        "Old", "B", "New", "B", DeviceState.IN_USE))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value())
                        .isEqualTo(HttpStatus.CONFLICT.value()));
    }

    @Test
    void whenLeavingInUse_nameMayChange() {
        DeviceRules.validateInUseIdentityLocks("N", "B", "New", "B", DeviceState.AVAILABLE);
    }

    @Test
    void whenTransitioningToInUseWithSameIdentity_allowed() {
        DeviceRules.validateInUseIdentityLocks("N", "B", "N", "B", DeviceState.IN_USE);
    }

    @Test
    void whenNotInUse_brandMayChange() {
        DeviceRules.validateInUseIdentityLocks("N", "Old", "N", "New", DeviceState.INACTIVE);
    }
}
