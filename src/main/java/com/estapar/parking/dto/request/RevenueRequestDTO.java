package com.estapar.parking.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RevenueRequestDTO {
    private LocalDate date;
    private String sector;
}