package com.estapar.parking.service;

import com.estapar.parking.entity.ParkingSession;
import com.estapar.parking.entity.Sector;
import com.estapar.parking.entity.Spot;
import com.estapar.parking.repository.ParkingSessionRepository;
import com.estapar.parking.repository.SectorRepository;
import com.estapar.parking.repository.SpotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ParkingService {

    private final SectorRepository sectorRepository;
    private final SpotRepository spotRepository;
    private final ParkingSessionRepository parkingSessionRepository;

    @Autowired
    public ParkingService(SectorRepository sectorRepository, SpotRepository spotRepository, ParkingSessionRepository parkingSessionRepository) {
        this.sectorRepository = sectorRepository;
        this.spotRepository = spotRepository;
        this.parkingSessionRepository = parkingSessionRepository;
    }

    private boolean isSectorFull(String sectorName) {
        Sector sector = sectorRepository.findByName(sectorName);
        if (sector == null) {
            return false;
        }
        long occupiedSpots = spotRepository.countBySectorNameAndOccupied(sectorName, true);
        return occupiedSpots >= sector.getMaxCapacity();
    }

    private double applyDynamicPricing(double basePrice, String sectorName) {
        Sector sector = sectorRepository.findByName(sectorName);
        if (sector == null) {
            return basePrice;
        }
        long occupiedSpots = spotRepository.countBySectorNameAndOccupied(sectorName, true);
        double occupancyRate = (double) occupiedSpots / sector.getMaxCapacity();

        if (occupancyRate < 0.25) {
            return basePrice * 0.9;
        } else if (occupancyRate <= 0.50) {
            return basePrice;
        } else if (occupancyRate <= 0.75) {
            return basePrice * 1.1;
        } else {
            return basePrice * 1.25;
        }
    }

    private double calculatePrice(ParkingSession session) {
        if (session == null || session.getEntryTime() == null || session.getExitTime() == null || session.getSpot() == null) {
            return 0.0;
        }
        long minutesParked = Duration.between(session.getEntryTime(), session.getExitTime()).toMinutes();
        double basePrice = session.getSpot().getSector().getBasePrice();
        double dynamicPrice = applyDynamicPricing(basePrice, session.getSpot().getSector().getName());
        return dynamicPrice * (minutesParked / 60.0);
    }

    @Transactional
    public void handleEntry(String licensePlate, LocalDateTime entryTime) {
        Optional<Spot> optionalSpot = spotRepository.findFirstAvailableSpot();

        if (optionalSpot.isPresent()) {
            Spot spot = optionalSpot.get();
            String sectorName = spot.getSector().getName();

            if (isSectorFull(sectorName)) {
                throw new SectorFullException("Setor " + sectorName + " está lotado.");
            }

            double finalPrice = applyDynamicPricing(spot.getSector().getBasePrice(), sectorName);

            ParkingSession session = new ParkingSession();
            session.setLicensePlate(licensePlate);
            session.setEntryTime(entryTime);
            session.setSpot(spot);
            session.setSector(spot.getSector());
            parkingSessionRepository.save(session);

            spot.setOccupied(true);
            spot.setCurrentParkingSession(session);
            spotRepository.save(spot);

            System.out.println("Veículo " + licensePlate + " entrou no setor " + sectorName + " às " + entryTime + " com preço base de: " + finalPrice);
        } else {
            throw new NoAvailableSpotException("Não há vagas disponíveis.");
        }
    }

    @Transactional
    public void handleExit(String licensePlate, LocalDateTime exitTime) {
        Optional<ParkingSession> optionalSession = parkingSessionRepository.findByLicensePlateAndExitTimeIsNull(licensePlate);
        if (optionalSession.isPresent()) {
            ParkingSession session = optionalSession.get();
            session.setExitTime(exitTime);

            Spot spot = session.getSpot();
            if (spot != null) {
                spot.setOccupied(false);
                spot.setCurrentParkingSession(null);
                spotRepository.save(spot);
            }

            double price = calculatePrice(session);
            session.setPrice(price);
            session.setDuration(Duration.between(session.getEntryTime(), exitTime)); //Calcula a duração
            parkingSessionRepository.save(session);

            System.out.println("Veículo " + licensePlate + " saiu às " + exitTime + ", preço: " + price);
        } else {
            System.out.println("Veículo " + licensePlate + " não encontrado.");
        }
    }

    //Exceções customizadas
    public static class SectorFullException extends RuntimeException {
        public SectorFullException(String message) {
            super(message);
        }
    }

    public static class NoAvailableSpotException extends RuntimeException {
        public NoAvailableSpotException(String message) {
            super(message);
        }
    }
}
