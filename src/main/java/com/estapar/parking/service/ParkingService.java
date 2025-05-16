package com.estapar.parking.service;

import com.estapar.parking.entity.Sector;
import com.estapar.parking.entity.Spot;
import com.estapar.parking.entity.ParkingSession;
import com.estapar.parking.repository.PriceRepository;
import com.estapar.parking.repository.SectorRepository;
import com.estapar.parking.repository.SpotRepository;
import com.estapar.parking.repository.ParkingSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.Duration;
import com.estapar.parking.exception.SectorFullException;

@Service
public class ParkingService {

    private final SectorRepository sectorRepository;
    private final SpotRepository spotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final PriceRepository priceRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${simulator.url}")
    private String simulatorUrl;

    public ParkingService(SectorRepository sectorRepository, SpotRepository spotRepository, ParkingSessionRepository sessionRepository, PriceRepository priceRepository, RestTemplate restTemplate, ObjectMapper objectMapper) { // Adicionado priceRepository ao construtor
        this.sectorRepository = sectorRepository;
        this.spotRepository = spotRepository;
        this.sessionRepository = sessionRepository;
        this.priceRepository = priceRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeGarageDataOnStartup() {
        fetchAndSaveGarageData();
    }

    private void fetchAndSaveGarageData() {
        String garageUrl = simulatorUrl + "/garage";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(garageUrl, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode garageData = root.get("garage");
                JsonNode spotsData = root.get("spots");

                sectorRepository.deleteAll();
                spotRepository.deleteAll();

                for (JsonNode sectorNode : garageData) {
                    Sector sector = objectMapper.treeToValue(sectorNode, Sector.class);
                    sectorRepository.save(sector);
                }

                for (JsonNode spotNode : spotsData) {
                    Spot spot = objectMapper.treeToValue(spotNode, Spot.class);
                    spotRepository.save(spot);
                }
                System.out.println("Dados da garagem inicializados com sucesso.");
            } else {
                System.err.println("Falha ao obter dados da garagem do simulador: " + response.getStatusCode());
            }
        } catch (IOException e) {
            System.err.println("Erro ao processar dados da garagem: " + e.getMessage());
        }
    }

    public ResponseEntity<String> handleWebhookEvent(@RequestBody JsonNode event) {
        String eventType = event.get("event_type").asText();
        String licensePlate = event.get("license_plate").asText();

        switch (eventType) {
            case "ENTRY":
                LocalDateTime entryTime = LocalDateTime.parse(event.get("entry_time").asText(), DateTimeFormatter.ISO_DATE_TIME);
                handleEntry(licensePlate, entryTime);
                break;
            case "PARKED":
                double lat = event.get("lat").asDouble();
                double lng = event.get("lng").asDouble();
                handleParked(licensePlate, lat, lng);
                break;
            case "EXIT":
                LocalDateTime exitTime = LocalDateTime.parse(event.get("exit_time").asText(), DateTimeFormatter.ISO_DATE_TIME);
                handleExit(licensePlate, exitTime);
                break;
            default:
                System.out.println("Tipo de evento webhook desconhecido: " + eventType);
                return ResponseEntity.badRequest().body("Tipo de evento desconhecido");
        }
        return ResponseEntity.ok("Evento " + eventType + " processado.");
    }

    void handleEntry(String licensePlate, LocalDateTime entryTime) {
        Optional<Spot> spot = spotRepository.findFirstAvailableSpot();
        if (spot.isPresent()) {
            ParkingSession session = new ParkingSession();
            session.setLicensePlate(licensePlate);
            session.setEntryTime(entryTime);
            session.setSpot(spot.get());
            sessionRepository.save(session);
            System.out.println("Veículo " + licensePlate + " entrou às " + entryTime);
        } else {
            System.out.println("Setor Lotado");
            throw new SectorFullException("Setor Lotado"); // Lança a exceção SectorFullException
        }
    }

    private void handleParked(String licensePlate, double lat, double lng) {
        Optional<Spot> optSpot = Optional.ofNullable(spotRepository.findByLatAndLng(lat, lng));
        Optional<ParkingSession> optSession = sessionRepository.findByLicensePlateAndExitTimeIsNull(licensePlate);

        if (optSpot.isPresent() && optSession.isPresent()) {
            Spot spot = optSpot.get();
            ParkingSession session = optSession.get();
            if (spot.isOccupied()) {
                System.out.println("Vaga já ocupada: " + spot.getId());
                return;
            }
            spot.setOccupied(true);
            spot.setCurrentParkingSession(session);
            spotRepository.save(spot);
            session.setSpot(spot);
            sessionRepository.save(session);
            System.out.println("Veículo " + licensePlate + " estacionou na vaga " + spot.getId() + ".");
        } else {
            System.out.println("Não foi possível estacionar " + licensePlate + " na vaga (vaga não encontrada ou ocupada).");
        }
    }

    void handleExit(String licensePlate, LocalDateTime exitTime) {
        Optional<ParkingSession> optSession = sessionRepository.findByLicensePlateAndExitTimeIsNull(licensePlate);
        if (optSession.isPresent()) {
            ParkingSession session = optSession.get();
            session.setExitTime(exitTime);

            Spot spot = session.getSpot();
            if (spot != null) {
                spot.setOccupied(false);
                spot.setCurrentParkingSession(null);
                spotRepository.save(spot);
            }
            Duration duration = Duration.between(session.getEntryTime(), exitTime);
            session.setDuration(duration);
            double price = calculatePrice(session);
            session.setPrice(price);
            sessionRepository.save(session);

            System.out.println("Veículo " + licensePlate + " saiu às " + exitTime + ", preço: " + price + ".");
        } else {
            System.out.println("Veículo " + licensePlate + " não encontrado ou não estava estacionado.");
        }
    }

    private double calculatePrice(ParkingSession session) {
        if (session.getEntryTime() == null || session.getExitTime() == null || session.getSpot() == null) {
            return 0.0;
        }
        long minutesParked = session.getDuration().toMinutes();
        Sector sector = session.getSpot().getSector();

        if (sector == null) {
            return 0.0;
        }

        double basePrice = sector.getBasePrice();
        double dynamicPrice = applyDynamicPricing(basePrice, sector.getName());
        return dynamicPrice * (minutesParked / 60.0);
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

    public ResponseEntity<?> getVehicleStatus(String licensePlate) {
        Optional<ParkingSession> optSession = sessionRepository.findByLicensePlateAndExitTimeIsNull(licensePlate);
        if (optSession.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Veículo não encontrado"));
        }
        ParkingSession session = optSession.get();

        String timeParked = session.getDuration() != null
                ? String.format("%02d:%02d:%02d", session.getDuration().toHours(), session.getDuration().toMinutesPart(), session.getDuration().toSecondsPart())
                : "N/A";

        Map<String, Object> response = new HashMap<>();
        response.put("license_plate", session.getLicensePlate());
        response.put("price_until_now", session.getPrice() != null ? session.getPrice() : 0.00);
        response.put("entry_time", session.getEntryTime() != null ? session.getEntryTime().format(DateTimeFormatter.ISO_DATE_TIME) : null);
        response.put("time_parked", timeParked);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> getSpotStatus(double lat, double lng) {
        Optional<Spot> optSpot = Optional.ofNullable(spotRepository.findByLatAndLng(lat, lng));
        if (optSpot.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Vaga não encontrada"));
        }
        Spot spot = optSpot.get();
        Optional<ParkingSession> optSession =  (spot.getCurrentParkingSession() != null) ? Optional.of(spot.getCurrentParkingSession()) : Optional.empty();

        String timeParked = optSession.isPresent() && spot.isOccupied()
                ? String.format("%02d:%02d:%02d", optSession.get().getDuration().toHours(), optSession.get().getDuration().toMinutesPart(), optSession.get().getDuration().toSecondsPart())
                : "N/A";

        Map<String, Object> response = new HashMap<>();
        response.put("occupied", spot.isOccupied());
        response.put("entry_time", optSession.isPresent() ? optSession.get().getEntryTime().format(DateTimeFormatter.ISO_DATE_TIME) : null);
        response.put("time_parked", timeParked);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> getRevenue(String dateStr, String sectorName) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr, dateFormatter);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Formato de data inválido (AAAA-MM-DD)"));
        }

        Sector sector = sectorRepository.findByName(sectorName);
        if (sector == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Setor não encontrado"));
        }

        List<ParkingSession> sessions = sessionRepository.findAll().stream()
                .filter(s -> s.getExitTime() != null &&
                        s.getExitTime().toLocalDate().isEqual(date) &&
                        s.getSpot() != null &&
                        s.getSpot().getSector().getName().equals(sectorName) &&
                        s.getPrice() != null)
                .collect(Collectors.toList());

        double totalRevenue = sessions.stream().mapToDouble(ParkingSession::getPrice).sum();

        Map<String, Object> response = new HashMap<>();
        response.put("amount", totalRevenue);
        response.put("currency", "BRL");
        response.put("timestamp", LocalDateTime.now(java.time.Clock.systemDefaultZone()).format(DateTimeFormatter.ISO_DATE_TIME));
        return ResponseEntity.ok(response);
    }

    public Optional<Spot> findFirstAvailableSpot() {
        return spotRepository.findFirstByOccupiedFalse();
    }
}

