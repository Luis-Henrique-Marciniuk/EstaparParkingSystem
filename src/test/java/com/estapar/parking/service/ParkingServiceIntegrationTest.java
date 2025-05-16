package com.estapar.parking.service;

import com.estapar.parking.entity.ParkingSession;
import com.estapar.parking.entity.Sector;
import com.estapar.parking.entity.Spot;
import com.estapar.parking.repository.ParkingSessionRepository;
import com.estapar.parking.repository.SectorRepository;
import com.estapar.parking.repository.SpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import com.estapar.parking.exception.SectorFullException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(ParkingService.class)
public class ParkingServiceIntegrationTest {

    @Autowired
    private ParkingService parkingService;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private SpotRepository spotRepository;

    @Autowired
    private ParkingSessionRepository parkingSessionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Sector sectorA;
    private Spot spot1A;
    private Spot spot2A;

    @BeforeEach
    public void setUp() {
        sectorA = new Sector("A", 10.0, 2, null, null, 120);
        entityManager.persist(sectorA);

        spot1A = new Spot(1.0, 1.0, sectorA);
        spot2A = new Spot(2.0, 2.0, sectorA);
        entityManager.persist(spot1A);
        entityManager.persist(spot2A);

        entityManager.flush();
    }

    @Test
    public void testHandleEntrySuccess() {
        LocalDateTime entryTime = LocalDateTime.now();
        parkingService.handleEntry("ABC-123", entryTime);

        Spot occupiedSpot = spotRepository.findById(spot1A.getId()).orElse(null);
        assertNotNull(occupiedSpot);
        assertTrue(occupiedSpot.isOccupied());

        List<ParkingSession> sessions = parkingSessionRepository.findAll();
        assertEquals(1, sessions.size());
        ParkingSession session = sessions.get(0);
        assertEquals("ABC-123", session.getLicensePlate());
        assertEquals(entryTime, session.getEntryTime());
        assertEquals(spot1A.getId(), session.getSpot().getId());
    }

    @Test
    public void testHandleEntrySectorFull() {
        parkingService.handleEntry("ABC-123", LocalDateTime.now());
        parkingService.handleEntry("DEF-456", LocalDateTime.now());

        assertThrows(SectorFullException.class, () -> {
            parkingService.handleEntry("GHI-789", LocalDateTime.now());
        });
    }

    @Test
    public void testHandleExitSuccess() {
        LocalDateTime entryTime = LocalDateTime.now().minusHours(1);
        parkingService.handleEntry("ABC-123", entryTime);

        LocalDateTime exitTime = LocalDateTime.now();
        parkingService.handleExit("ABC-123", exitTime);

        Spot freedSpot = spotRepository.findById(spot1A.getId()).orElse(null);
        assertNotNull(freedSpot);
        assertFalse(freedSpot.isOccupied());

        ParkingSession session = parkingSessionRepository.findByLicensePlateAndExitTimeIsNull("ABC-123").orElse(null);
        assertNull(session);

        List<ParkingSession> sessions = parkingSessionRepository.findAll();
        assertEquals(1, sessions.size());
        ParkingSession sessionFinal = sessions.get(0);
        assertNotNull(sessionFinal.getPrice());
    }

    @Test
    public void testHandleExitVehicleNotFound() {
        parkingService.handleExit("XYZ-987", LocalDateTime.now());
    }
}
