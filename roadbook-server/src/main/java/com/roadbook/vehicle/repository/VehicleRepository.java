package com.roadbook.vehicle.repository;

import com.roadbook.vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE Vehicle v SET v.isDefault = 0 WHERE v.userId = :userId AND v.isDefault = 1")
    int clearDefaultsByUserId(Long userId);
}
