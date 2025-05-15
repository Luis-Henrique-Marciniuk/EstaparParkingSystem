package com.estapar.parking.dto.request;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpotStatusRequestDTO {
    private double lat;
    private double lng;
}