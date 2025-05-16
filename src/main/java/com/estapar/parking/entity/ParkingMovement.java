package com.estapar.parking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class ParkingMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "license_plate", nullable = false)
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "spot_id", nullable = false)
    private ParkingSpot parkingSpot;

    @Column(nullable = false)
    private LocalDateTime entryTime;

    private LocalDateTime exitTime;

    private Double pricePaid;

}