package com.estapar.parking.service;

import com.estapar.parking.entity.Garage;
import com.estapar.parking.entity.ParkingSpot;
import com.estapar.parking.repository.GarageSectorRepository;
import com.estapar.parking.repository.ParkingSpotRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GarageInitializer {

    private final GarageSectorRepository sectorRepository;
    private final ParkingSpotRepository spotRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        String url = "http://localhost:3000/garage";
        Map response = restTemplate.getForObject(url, Map.class);

        if (response == null) return;

        List<Map<String, Object>> sectors = (List<Map<String, Object>>) response.get("garage");
        List<Map<String, Object>> spots = (List<Map<String, Object>>) response.get("spots");

//        for (Map<String, Object> s : sectors) {
//            Garage sector = new Garage(
//                    (String) s.get("sector"),
//                    ((Number) s.get("basePrice")).doubleValue(),
//                    (Integer) s.get("max_capacity"),
//                    LocalTime.parse((String) s.get("open_hour")),
//                    LocalTime.parse((String) s.get("close_hour")),
//                    (Integer) s.get("duration_limit_minutes")
//            );
//            sectorRepository.save(sector);
//        }

        for (Map<String, Object> sp : spots) {
            String sectorKey = (String) sp.get("sector");
            Garage sector = sectorRepository.findById(sectorKey).orElse(null);
            if (sector == null) continue;

            ParkingSpot spot = new ParkingSpot();
            spot.setLat(((Number) sp.get("lat")).doubleValue());
            spot.setLng(((Number) sp.get("lng")).doubleValue());
//            spot.setSector(sector);
            spotRepository.save(spot);
        }

        System.out.println("Garage config loaded and saved.");
    }
}