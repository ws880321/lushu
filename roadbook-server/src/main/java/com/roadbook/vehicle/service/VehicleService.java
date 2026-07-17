package com.roadbook.vehicle.service;

import com.roadbook.common.ErrorCode;
import com.roadbook.vehicle.entity.Vehicle;
import com.roadbook.vehicle.repository.VehicleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    /**
     * Create a new vehicle for the user.
     *
     * @param userId  the authenticated user ID
     * @param vehicle vehicle entity with fields to save (id must be null)
     * @return the saved vehicle with generated ID
     */
    @Transactional
    public Vehicle create(Long userId, Vehicle vehicle) {
        vehicle.setUserId(userId);
        vehicle.setIsDefault(0);
        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("User {} created vehicle {}", userId, saved.getId());
        return saved;
    }

    /**
     * List all vehicles owned by the user.
     */
    public List<Vehicle> listByUser(Long userId) {
        return vehicleRepository.findByUserId(userId);
    }

    /**
     * Update an existing vehicle. Only the owner may update.
     *
     * @param userId    the authenticated user ID
     * @param vehicleId the vehicle ID to update
     * @param update    the updated fields (userId and id are ignored)
     * @return the updated vehicle
     * @throws IllegalArgumentException if the vehicle is not found or not owned by the user
     */
    @Transactional
    public Vehicle update(Long userId, Long vehicleId, Vehicle update) {
        Vehicle existing = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("VEHICLE_NOT_FOUND"));

        if (!existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("VEHICLE_NOT_OWNED");
        }

        existing.setName(update.getName());
        existing.setBrand(update.getBrand());
        existing.setFuelType(update.getFuelType());
        existing.setTankCapacity(update.getTankCapacity());
        existing.setAvgConsumption(update.getAvgConsumption());
        existing.setRangeFull(update.getRangeFull());
        existing.setPlateNumber(update.getPlateNumber());

        Vehicle saved = vehicleRepository.save(existing);
        log.info("User {} updated vehicle {}", userId, vehicleId);
        return saved;
    }

    /**
     * Delete a vehicle. Only the owner may delete.
     *
     * @param userId    the authenticated user ID
     * @param vehicleId the vehicle ID to delete
     * @throws IllegalArgumentException if the vehicle is not found or not owned by the user
     */
    @Transactional
    public void delete(Long userId, Long vehicleId) {
        Vehicle existing = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("VEHICLE_NOT_FOUND"));

        if (!existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("VEHICLE_NOT_OWNED");
        }

        vehicleRepository.delete(existing);
        log.info("User {} deleted vehicle {}", userId, vehicleId);
    }

    /**
     * Set a vehicle as the user's default. All other vehicles for this user
     * are set to non-default first, then the target vehicle is set as default.
     *
     * @param userId    the authenticated user ID
     * @param vehicleId the vehicle ID to set as default
     * @return the updated vehicle
     * @throws IllegalArgumentException if the vehicle is not found or not owned by the user
     */
    @Transactional
    public Vehicle setDefault(Long userId, Long vehicleId) {
        Vehicle target = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("VEHICLE_NOT_FOUND"));

        if (!target.getUserId().equals(userId)) {
            throw new IllegalArgumentException("VEHICLE_NOT_OWNED");
        }

        // Clear existing defaults for this user
        List<Vehicle> userVehicles = vehicleRepository.findByUserId(userId);
        for (Vehicle v : userVehicles) {
            if (v.getIsDefault() == 1) {
                v.setIsDefault(0);
                vehicleRepository.save(v);
            }
        }

        // Set the target as default
        target.setIsDefault(1);
        Vehicle saved = vehicleRepository.save(target);
        log.info("User {} set vehicle {} as default", userId, vehicleId);
        return saved;
    }
}
