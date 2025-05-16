package com.estapar.parking.repository;

import com.estapar.parking.entity.ParkingSession;
import com.estapar.parking.entity.Spot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {
    Optional<ParkingSession> findByLicensePlateAndExitTimeIsNull(String licensePlate);
    Optional<ParkingSession> findBySpotAndExitTimeIsNull(Spot spot);
    List<ParkingSession> findAllBySectorNameAndExitTimeBetween(String sectorName, LocalDateTime start, LocalDateTime end);
}
