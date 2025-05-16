package com.estapar.parking.service;

import com.estapar.parking.dto.WebhookEventDTO;
import com.estapar.parking.entity.Garage;
import com.estapar.parking.entity.ParkingSession;
import com.estapar.parking.entity.Spot;
import com.estapar.parking.repository.GarageSectorRepository;
import com.estapar.parking.repository.ParkingSessionRepository;
import com.estapar.parking.repository.SpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WebhookEventService {

    private final SpotRepository spotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final GarageSectorRepository sectorRepository;

    public void processEvent(WebhookEventDTO event) {
        switch (event.getEventType()) {
            case "ENTRY" -> handleEntry(event);
            case "PARKED" -> handleParked(event);
            case "EXIT" -> handleExit(event);
        }
    }

    private void handleEntry(WebhookEventDTO event) {
        ParkingSession session = new ParkingSession();
        session.setLicensePlate(event.getLicensePlate());
        session.setEntryTime(event.getEntryTime());
        sessionRepository.save(session);
    }

    private void handleParked(WebhookEventDTO event) {
        Optional<Spot> optSpot = spotRepository.findByLatAndLng(event.getLat(), event.getLng());
        Optional<ParkingSession> optSession = sessionRepository.findByLicensePlateAndExitTimeIsNull(event.getLicensePlate());

        if (optSpot.isPresent() && optSession.isPresent()) {
            Spot spot = optSpot.get();
            ParkingSession session = optSession.get();

            spot.setOccupied(true);
            session.setSpot(spot);
//            session.setSector(spot.getSector());
            spotRepository.save(spot);
            sessionRepository.save(session);
        }
    }

    private void handleExit(WebhookEventDTO event) {
        Optional<ParkingSession> optSession = sessionRepository.findByLicensePlateAndExitTimeIsNull(event.getLicensePlate());
        if (optSession.isPresent()) {
            ParkingSession session = optSession.get();
            session.setExitTime(event.getExitTime());

            Spot spot = session.getSpot();
            if (spot != null) {
                spot.setOccupied(false);
                spotRepository.save(spot);
            }

            double price = calculatePrice(session);
            session.setPrice(price);

            sessionRepository.save(session);
        }
    }

    private double calculatePrice(ParkingSession session) {
        long minutes = Duration.between(session.getEntryTime(), session.getExitTime()).toMinutes();
        Garage sector = session.getSector();

        if (sector == null) return 0.0;

        double basePrice = sector.getBasePrice();

        if (minutes <= 30) {
            return basePrice * 0.5;
        } else if (minutes <= 60) {
            return basePrice;
        } else {
            return basePrice + ((minutes - 60) / 30.0) * (basePrice * 0.3);
        }
    }
}