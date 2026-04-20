package com.devices.repo;

import com.devices.domain.Device;
import com.devices.domain.DeviceState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Page<Device> findByBrandIgnoreCase(String brand, Pageable pageable);

    Page<Device> findByState(DeviceState state, Pageable pageable);

    Page<Device> findByBrandIgnoreCaseAndState(String brand, DeviceState state, Pageable pageable);
}
