package com.estapar.parking.service;

import com.estapar.parking.dto.request.PlateStatusRequestDTO;
import com.estapar.parking.dto.request.RevenueRequestDTO;
import com.estapar.parking.dto.request.SpotStatusRequestDTO;
import com.estapar.parking.dto.response.PlateStatusResponseDTO;
import com.estapar.parking.dto.response.RevenueResponseDTO;
import com.estapar.parking.dto.response.SpotStatusResponseDTO;
import com.estapar.parking.entity.ParkingSession;
import com.estapar.parking.entity.Spot;
import com.estapar.parking.repository.ParkingSessionRepository;
import com.estapar.parking.repository.SpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QueryService {

    private final ParkingSessionRepository sessionRepository;
    private final SpotRepository spotRepository;

    public PlateStatusResponseDTO getPlateStatus(PlateStatusRequestDTO request) {
        Optional<ParkingSession> optSession = sessionRepository.findByLicensePlateAndExitTimeIsNull(request.getLicensePlate());

        if (optSession.isEmpty()) return null;

        ParkingSession session = optSession.get();
        PlateStatusResponseDTO response = new PlateStatusResponseDTO();
        response.setLicensePlate(session.getLicensePlate());
        response.setEntryTime(session.getEntryTime());
        response.setTimeParked(LocalDateTime.now());
        response.setPriceUntilNow(0.0); // Pode implementar lógica se necessário

        return response;
    }

    public SpotStatusResponseDTO getSpotStatus(SpotStatusRequestDTO request) {
        Optional<Spot> optSpot = spotRepository.findByLatAndLng(request.getLat(), request.getLng());

        SpotStatusResponseDTO response = new SpotStatusResponseDTO();
        if (optSpot.isPresent()) {
            Spot spot = optSpot.get();
            response.setOcupied(spot.isOccupied());

            Optional<ParkingSession> optSession = sessionRepository.findBySpotAndExitTimeIsNull(spot);
            optSession.ifPresent(session -> {
                response.setEntryTime(session.getEntryTime());
                response.setTimeParked(LocalDateTime.now());
            });
        }
        return response;
    }

    public RevenueResponseDTO getRevenue(RevenueRequestDTO request) {
        LocalDate targetDate = LocalDate.parse(request.getDate(), DateTimeFormatter.ISO_DATE);

        double total = sessionRepository.findAllBySectorNameAndExitTimeBetween(
                request.getSector(),
                targetDate.atStartOfDay(),
                targetDate.plusDays(1).atStartOfDay()
        ).stream().mapToDouble(ParkingSession::getPrice).sum();

        RevenueResponseDTO response = new RevenueResponseDTO();
        response.setAmount(total);
        response.setTimestamp(LocalDateTime.now());

        return response;
    }
}