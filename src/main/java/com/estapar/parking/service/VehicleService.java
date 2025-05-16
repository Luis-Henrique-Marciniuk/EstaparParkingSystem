//package com.estapar.parking.service;
//
//import com.estapar.parking.dto.PlateStatusRequest;
//import com.estapar.parking.dto.PlateStatusResponse;
//import com.estapar.parking.model.Sector;
//import com.estapar.parking.model.Vehicle;
//import com.estapar.parking.repository.SectorRepository;
//import com.estapar.parking.repository.VehicleRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//public class VehicleService {
//
//    private final VehicleRepository vehicleRepository;
//    private final SectorRepository sectorRepository;
//
//    public PlateStatusResponse getPlateStatus(PlateStatusRequest request) {
//        String licensePlate = request.getLicensePlate();
//
//        Optional<Vehicle> optionalVehicle = vehicleRepository.findByLicensePlate(licensePlate);
//
//        if (optionalVehicle.isEmpty()) {
//            throw new RuntimeException("Veículo não encontrado: " + licensePlate);
//        }
//
//        Vehicle vehicle = optionalVehicle.get();
//
//        if (vehicle.getEntryTime() == null) {
//            throw new RuntimeException("Veículo ainda não entrou.");
//        }
//
//        LocalDateTime now = LocalDateTime.now();
//        Duration duration = Duration.between(vehicle.getEntryTime(), now);
//
//        Optional<Sector> optionalSector = sectorRepository.findById(vehicle.getSector());
//
//        if (optionalSector.isEmpty()) {
//            throw new RuntimeException("Setor não encontrado: " + vehicle.getSector());
//        }
//
//        Sector sector = optionalSector.get();
//        double price = calculatePrice(sector, duration.toMinutes());
//
//        return PlateStatusResponse.builder()
//                .licensePlate(vehicle.getLicensePlate())
//                .entryTime(vehicle.getEntryTime())
//                .timeParked(now)
//                .priceUntilNow(price)
//                .build();
//    }
//
//    private double calculatePrice(Sector sector, long minutesParked) {
//        double basePricePerHour = sector.getBasePrice();
//        double hours = minutesParked / 60.0;
//
//        // Aqui pode-se aplicar regras dinâmicas de preço no momento da entrada se quiser.
//        return roundToTwoDecimals(basePricePerHour * hours);
//    }
//
//    private double roundToTwoDecimals(double value) {
//        return Math.round(value * 100.0) / 100.0;
//    }
//}
