package com.estapar.parking.controller;


import com.estapar.parking.dto.request.PlateStatusRequestDTO;
import com.estapar.parking.dto.response.PlateStatusResponseDTO;
import com.estapar.parking.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/plate-status")
@RequiredArgsConstructor
public class PlateStatusController {

    private final QueryService queryService;

    @PostMapping
    public ResponseEntity<PlateStatusResponseDTO> getPlateStatus(@RequestBody PlateStatusRequestDTO request) {
        return ResponseEntity.ok(queryService.getPlateStatus(request));
    }
}