package com.estapar.parking.repository;

import com.estapar.parking.entity.Garage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GarageSectorRepository extends JpaRepository<Garage, String> {
}