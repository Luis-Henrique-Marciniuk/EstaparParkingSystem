package com.estapar.parking.service;

import com.estapar.parking.entity.GarageSector;
import com.estapar.parking.entity.ParkingSpot;
import com.estapar.parking.repository.GarageSectorRepository;
import com.estapar.parking.repository.ParkingSpotRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class SetupService {

    private final GarageSectorRepository sectorRepository;
    private final ParkingSpotRepository spotRepository;
    private final ObjectMapper objectMapper;

    public void importGarageData() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:3000/garage";
        JsonNode response = restTemplate.getForObject(url, JsonNode.class);

        for (JsonNode sectorNode : response.get("garage")) {
            GarageSector sector = new GarageSector();
            sector.setName(sectorNode.get("sector").asText());
            sector.setBasePrice(sectorNode.get("basePrice").asDouble());
            sector.setMaxCapacity(sectorNode.get("max_capacity").asInt());
            sector.setOpenHour(LocalTime.parse(sectorNode.get("open_hour").asText()));
            sector.setCloseHour(LocalTime.parse(sectorNode.get("close_hour").asText()));
            sector.setDurationLimitMinutes(sectorNode.get("duration_limit_minutes").asInt());
            sectorRepository.save(sector);
        }

        for (JsonNode spotNode : response.get("spots")) {
            ParkingSpot spot = new ParkingSpot();
            spot.setLat(spotNode.get("lat").asDouble());
            spot.setLng(spotNode.get("lng").asDouble());
            spot.setOccupied(false);
            String sectorName = spotNode.get("sector").asText();
            GarageSector sector = sectorRepository.findByName(sectorName).orElseThrow();
            spot.setSector(sector);
            spotRepository.save(spot);
        }
    }
}