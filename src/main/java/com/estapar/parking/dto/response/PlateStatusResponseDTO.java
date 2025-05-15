package com.estapar.parking.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PlateStatusResponseDTO {
    private String licensePlate;
    private double priceUntilNow;
    private LocalDateTime entryTime;
    private LocalDateTime timeParked;
}