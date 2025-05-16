package com.estapar.parking.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Vehicle {

    @Id
    private String licensePlate;
}