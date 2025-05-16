package com.estapar.parking.repository;

import com.estapar.parking.entity.Garage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SectorRepository extends JpaRepository<Garage, Long> {
    Garage findByName(String name);
}
