package com.estapar.parking.controller;

import com.estapar.parking.dto.request.RevenueRequestDTO;
import com.estapar.parking.dto.response.RevenueResponseDTO;
import com.estapar.parking.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/revenue")
@RequiredArgsConstructor
public class RevenueController {

    private final QueryService queryService;

    @GetMapping
    public ResponseEntity<RevenueResponseDTO> getRevenue(@RequestBody RevenueRequestDTO request) {
        return ResponseEntity.ok(queryService.getRevenue(request));
    }
}