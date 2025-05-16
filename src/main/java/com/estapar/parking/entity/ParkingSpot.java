package com.estapar.parking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double lat;
    private Double lng;

    @Column(nullable = false)
    private boolean occupied = false;

    @ManyToOne
    @JoinColumn(name = "sector", nullable = false)
    private Garage garage;

}
