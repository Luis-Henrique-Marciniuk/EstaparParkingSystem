package com.estapar.parking.service;

import com.estapar.parking.entity.Garage;
import com.estapar.parking.entity.Vehicle;
import com.estapar.parking.repository.SectorRepository;
import com.estapar.parking.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ParkingService {

    private final SectorRepository sectorRepository;
    private final SpotRepository spotRepository;
    private final VehicleRepository vehicleRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${simulator.url}")
    private String simulatorUrl;

    public ParkingService(SectorRepository sectorRepository, SpotRepository spotRepository, VehicleRepository vehicleRepository, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.sectorRepository = sectorRepository;
        this.spotRepository = spotRepository;
        this.vehicleRepository = vehicleRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeGarageDataOnStartup() {
        fetchAndSaveGarageData();
    }

    public void fetchAndSaveGarageData() {
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

    public ResponseEntity<String> handleWebhookEvent(JsonNode event) {
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

    private void handleEntry(String licensePlate, LocalDateTime entryTime) {
        if (!vehicleRepository.existsById(licensePlate)) {
            Vehicle vehicle = new Vehicle(licensePlate, entryTime);
            vehicleRepository.save(vehicle);
            System.out.println("Veículo " + licensePlate + " entrou às " + entryTime);
        } else {
            System.out.println("Veículo " + licensePlate + " já está registrado.");
        }
    }

    private void handleParked(String licensePlate, double lat, double lng) {
        Spot spot = spotRepository.findByLatAndLng(lat, lng);
        Vehicle vehicle = vehicleRepository.findById(licensePlate).orElse(null);

        if (spot != null && vehicle != null && !spot.isOccupied()) {
            spot.setOccupied(true);
            spot.setEntryTime(LocalDateTime.now(java.time.Clock.systemDefaultZone()));
            spot.setLicensePlate(licensePlate); // Associe a placa à vaga
            spotRepository.save(spot);
            vehicle.setCurrentSpot(spot); // Associe a vaga ao veículo (se você adicionar esse campo na entidade Vehicle)
            vehicleRepository.save(vehicle);
            System.out.println("Veículo " + licensePlate + " estacionou na vaga " + spot.getId() + ".");
        } else {
            System.out.println("Não foi possível estacionar " + licensePlate + " na vaga (vaga não encontrada ou ocupada).");
        }
    }

    private void handleExit(String licensePlate, LocalDateTime exitTime) {
        Vehicle vehicle = vehicleRepository.findById(licensePlate).orElse(null);
        if (vehicle != null && vehicle.getCurrentSpot() != null) {
            vehicle.setExitTime(exitTime);
            double price = calculatePrice(vehicle);
            vehicle.setPricePaid(price);
            vehicleRepository.save(vehicle);

            Spot spot = vehicle.getCurrentSpot();
            spot.setOccupied(false);
            spot.setEntryTime(null);
            spot.setLicensePlate(null); // Desassocie a placa da vaga
            spotRepository.save(spot);
            vehicle.setCurrentSpot(null); // Desassocie a vaga do veículo
            vehicleRepository.save(vehicle);

            System.out.println("Veículo " + licensePlate + " saiu às " + exitTime + ", preço: " + price + ".");
        } else {
            System.out.println("Veículo " + licensePlate + " não encontrado ou não estava estacionado.");
        }
    }

    private double calculatePrice(Vehicle vehicle) {
        if (vehicle.getEntryTime() == null || vehicle.getExitTime() == null || vehicle.getCurrentSpot() == null) {
            return 0.0;
        }

        long minutesParked = ChronoUnit.MINUTES.between(vehicle.getEntryTime(), vehicle.getExitTime());
        Garage sector = sectorRepository.findByName(vehicle.getCurrentSpot().getSectorName());

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
        Vehicle vehicle = vehicleRepository.findById(licensePlate).orElse(null);
        if (vehicle == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Veículo não encontrado"));
        }
        String timeParked = (vehicle.getEntryTime() != null && vehicle.getExitTime() == null)
                ? formatDuration(vehicle.getEntryTime(), LocalDateTime.now(java.time.Clock.systemDefaultZone()))
                : (vehicle.getEntryTime() != null && vehicle.getExitTime() != null)
                ? formatDuration(vehicle.getEntryTime(), vehicle.getExitTime())
                : "N/A";

        Map<String, Object> response = new HashMap<>();
        response.put("license_plate", vehicle.getLicensePlate());
        response.put("price_until_now", vehicle.getPricePaid() != null ? vehicle.getPricePaid() : 0.00);
        response.put("entry_time", vehicle.getEntryTime() != null ? vehicle.getEntryTime().format(DateTimeFormatter.ISO_DATE_TIME) : null);
        response.put("time_parked", timeParked);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> getSpotStatus(double lat, double lng) {
        Spot spot = spotRepository.findByLatAndLng(lat, lng);
        if (spot == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Vaga não encontrada"));
        }
        String timeParked = spot.isOccupied() && spot.getEntryTime() != null
                ? formatDuration(spot.getEntryTime(), LocalDateTime.now(java.time.Clock.systemDefaultZone()))
                : "N/A";

        Map<String, Object> response = new HashMap<>();
        response.put("occupied", spot.isOccupied());
        response.put("entry_time", spot.getEntryTime() != null ? spot.getEntryTime().format(DateTimeFormatter.ISO_DATE_TIME) : null);
        response.put("time_parked", timeParked);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> getRevenue(String dateStr, String sectorName) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDateTime date;
        try {
            date = LocalDate.parse(dateStr, dateFormatter).atStartOfDay();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Formato de data inválido (AAAA-MM-DD)"));
        }

        Sector sector = sectorRepository.findByName(sectorName);
        if (sector == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Setor não encontrado"));
        }

        List<Vehicle> vehicles = vehicleRepository.findAll().stream()
                .filter(v -> v.getExitTime() != null &&
                        v.getExitTime().toLocalDate().isEqual(date.toLocalDate()) &&
                        v.getCurrentSpot() != null &&
                        v.getCurrentSpot().getSectorName().equals(sectorName) &&
                        v.getPricePaid() != null)
                .collect(Collectors.toList());

        double totalRevenue = vehicles.stream().mapToDouble(Vehicle::getPricePaid).sum();

        Map<String, Object> response = new HashMap<>();
        response.put("amount", totalRevenue);
        response.put("currency", "BRL");
        response.put("timestamp", LocalDateTime.now(java.time.Clock.systemDefaultZone()).format(DateTimeFormatter.ISO_DATE_TIME));
        return ResponseEntity.ok(response);
    }

    private String formatDuration(LocalDateTime start, LocalDateTime end) {
        long totalSeconds = ChronoUnit.SECONDS.between(start, end);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}