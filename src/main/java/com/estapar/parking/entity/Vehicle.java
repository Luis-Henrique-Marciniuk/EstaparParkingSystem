package com.estapar.parking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name="vehicle")
@Getter
@Setter
public class Vehicle {

    @Id
    private String licensePlate;

    public Vehicle(String licensePlate, LocalDateTime entryTime) {
        this.licensePlate = licensePlate;
        // this.entryTime = entryTime;
    }
}

