package com.estapar.parking.controller;

import com.estapar.parking.dto.request.SpotStatusRequestDTO;
import com.estapar.parking.dto.response.SpotStatusResponseDTO;
import com.estapar.parking.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/spot-status")
@RequiredArgsConstructor
public class SpotStatusController {

    private final QueryService queryService;

    @PostMapping
    public ResponseEntity<SpotStatusResponseDTO> getSpotStatus(@RequestBody SpotStatusRequestDTO request) {
        return ResponseEntity.ok(queryService.getSpotStatus(request));
    }
}