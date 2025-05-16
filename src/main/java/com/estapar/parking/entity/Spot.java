package com.estapar.parking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Spot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double lat;
    private Double lng;

    @Column(nullable = false)
    private boolean occupied = false;

    @ManyToOne
    @JoinColumn(name = "sector_name", nullable = false)
    private Sector sector;

    private LocalDateTime entryTime;

    private String licensePlate;

    public Spot(Double lat, Double lng, Sector sector) {
        this.lat = lat;
        this.lng = lng;
        this.sector = sector;
    }
}
