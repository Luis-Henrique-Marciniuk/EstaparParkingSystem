package com.estapar.parking.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Setter
public class SpotStatusResponseDTO {
    private boolean ocupied;
    private LocalDateTime entryTime;
//    private LocalDateTime timeParked;
    private Duration timeParked;

}