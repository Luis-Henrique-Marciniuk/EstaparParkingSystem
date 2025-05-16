package com.estapar.parking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Garage {
    @Id
    private String sector;

    @Column(nullable = false)
    private Double basePrice;

    @Column(nullable = false)
    private Integer maxCapacity;

    @Column(nullable = false)
    private String openHour;

    @Column(nullable = false)
    private String closeHour;

    private Integer durationLimitMinutes;

    @OneToMany(mappedBy = "garage")
    private List<ParkingSpot> parkingSpots;

}
