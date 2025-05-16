package com.estapar.parking.service;

import com.estapar.parking.entity.Garage;
import com.estapar.parking.entity.Spot;
import com.estapar.parking.repository.GarageSectorRepository;
import com.estapar.parking.repository.SpotRepository;
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
    private final SpotRepository spotRepository;
    private final ObjectMapper objectMapper;

    public void importGarageData() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:3000/garage";
        JsonNode response = restTemplate.getForObject(url, JsonNode.class);

        for (JsonNode sectorNode : response.get("garage")) {
            Garage sector = new Garage();
//            sector.setName(sectorNode.get("sector").asText());
            sector.setBasePrice(sectorNode.get("basePrice").asDouble());
            sector.setMaxCapacity(sectorNode.get("max_capacity").asInt());
            sector.setOpenHour(String.valueOf(LocalTime.parse(sectorNode.get("open_hour").asText())));
            sector.setCloseHour(String.valueOf(LocalTime.parse(sectorNode.get("close_hour").asText())));
            sector.setDurationLimitMinutes(sectorNode.get("duration_limit_minutes").asInt());
            sectorRepository.save(sector);
        }

        for (JsonNode spotNode : response.get("spots")) {
            Spot spot = new Spot();
            spot.setLat(spotNode.get("lat").asDouble());
            spot.setLng(spotNode.get("lng").asDouble());
            spot.setOccupied(false);
            String sectorName = spotNode.get("sector").asText();
//            Garage sector = sectorRepository.findByName(sectorName).orElseThrow();
//            spot.setSector(sector);
            spotRepository.save(spot);
        }
    }
}