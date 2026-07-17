package com.roadbook.vehicle.controller;

import com.roadbook.common.ApiResponse;
import com.roadbook.common.ErrorCode;
import com.roadbook.vehicle.entity.Vehicle;
import com.roadbook.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /**
     * Add a new vehicle for the current user.
     *
     * @param userId  authenticated user ID from JWT interceptor
     * @param vehicle vehicle data from request body
     * @return the created vehicle
     */
    @PostMapping
    public ApiResponse<Vehicle> create(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody Vehicle vehicle) {
        try {
            Vehicle saved = vehicleService.create(userId, vehicle);
            return ApiResponse.success(saved);
        } catch (IllegalArgumentException e) {
            log.warn("Vehicle creation failed for user {}: {}", userId, e.getMessage());
            return ApiResponse.error(ErrorCode.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * List all vehicles for the current user.
     *
     * @param userId authenticated user ID from JWT interceptor
     * @return list of vehicles
     */
    @GetMapping
    public ApiResponse<List<Vehicle>> list(@RequestAttribute("userId") Long userId) {
        List<Vehicle> vehicles = vehicleService.listByUser(userId);
        return ApiResponse.success(vehicles);
    }

    /**
     * Set a vehicle as the default for the current user.
     *
     * @param userId    authenticated user ID from JWT interceptor
     * @param vehicleId the vehicle ID to set as default
     * @return the updated vehicle
     */
    @PutMapping("/{id}/default")
    public ApiResponse<Vehicle> setDefault(
            @RequestAttribute("userId") Long userId,
            @PathVariable("id") Long vehicleId) {
        try {
            Vehicle updated = vehicleService.setDefault(userId, vehicleId);
            return ApiResponse.success(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Set default vehicle failed for user {}: {}", userId, e.getMessage());
            String message = e.getMessage();
            if ("VEHICLE_NOT_FOUND".equals(message)) {
                return ApiResponse.error(ErrorCode.NOT_FOUND);
            }
            return ApiResponse.error(ErrorCode.BAD_REQUEST, message);
        }
    }

    /**
     * Delete a vehicle by ID. Only the owner may delete.
     *
     * @param userId    authenticated user ID from JWT interceptor
     * @param vehicleId the vehicle ID to delete
     * @return success response
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @RequestAttribute("userId") Long userId,
            @PathVariable("id") Long vehicleId) {
        try {
            vehicleService.delete(userId, vehicleId);
            return ApiResponse.success(null);
        } catch (IllegalArgumentException e) {
            log.warn("Vehicle deletion failed for user {}: {}", userId, e.getMessage());
            String message = e.getMessage();
            if ("VEHICLE_NOT_FOUND".equals(message)) {
                return ApiResponse.error(ErrorCode.NOT_FOUND);
            }
            return ApiResponse.error(ErrorCode.BAD_REQUEST, message);
        }
    }
}
