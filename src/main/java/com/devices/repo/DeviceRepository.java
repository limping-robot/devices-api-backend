package com.devices.repo;

import com.devices.domain.Device;
import com.devices.domain.DeviceState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByBrandIgnoreCase(String brand);

    List<Device> findByState(DeviceState state);

    List<Device> findByBrandIgnoreCaseAndState(String brand, DeviceState state);
}
