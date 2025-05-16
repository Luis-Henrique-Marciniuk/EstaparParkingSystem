package com.estapar.parking.repository;

import com.estapar.parking.entity.Spot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpotRepository extends JpaRepository<Spot, Long> {
    Spot findByLatAndLng(double lat, double lng);
    List<Spot> findBySectorName(String sectorName);
    long countBySectorNameAndOccupied(String sectorName, boolean occupied);
}