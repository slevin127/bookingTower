package org.example.bookingtower.web.controller;

import org.example.bookingtower.domain.entity.Coworking;
import org.example.bookingtower.domain.entity.Workspace;
import org.example.bookingtower.infrastructure.repository.CoworkingRepository;
import org.example.bookingtower.infrastructure.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class WorkspaceCreationTest {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private CoworkingRepository coworkingRepository;

    @Test
    public void testWorkspaceCreation() {
        System.out.println("[DEBUG_LOG] Starting workspace creation test");

        // Create a test coworking first
        Coworking coworking = new Coworking();
        coworking.setName("Test Coworking");
        coworking.setAddress("Test Address");
        coworking.setTimezone("Europe/Moscow");
        coworking.setOpenFrom(LocalTime.of(9, 0));
        coworking.setOpenTo(LocalTime.of(21, 0));
        coworking.setActive(true);
        
        coworking = coworkingRepository.save(coworking);
        System.out.println("[DEBUG_LOG] Created test coworking with ID: " + coworking.getId());

        // Create a test workspace
        Workspace workspace = new Workspace();
        workspace.setName("Test Workspace");
        workspace.setDescription("Test Description");
        workspace.setSeatsTotal(10);
        workspace.setAmenities("Wi-Fi, Coffee");
        workspace.setPricePerHour(new BigDecimal("500.00"));
        workspace.setActive(true);
        workspace.setCoworking(coworking);

        // Save the workspace
        workspace = workspaceRepository.save(workspace);
        System.out.println("[DEBUG_LOG] Created test workspace with ID: " + workspace.getId());

        // Verify the workspace was saved
        assertNotNull(workspace.getId());
        assertEquals("Test Workspace", workspace.getName());
        assertEquals("Test Description", workspace.getDescription());
        assertEquals(Integer.valueOf(10), workspace.getSeatsTotal());
        assertEquals("Wi-Fi, Coffee", workspace.getAmenities());
        assertEquals(new BigDecimal("500.00"), workspace.getPricePerHour());
        assertTrue(workspace.getActive());
        assertNotNull(workspace.getCoworking());
        assertEquals(coworking.getId(), workspace.getCoworking().getId());

        // Verify it can be retrieved from database
        List<Workspace> allWorkspaces = workspaceRepository.findAll();
        System.out.println("[DEBUG_LOG] Total workspaces in database: " + allWorkspaces.size());
        
        boolean found = allWorkspaces.stream()
                .anyMatch(w -> "Test Workspace".equals(w.getName()));
        assertTrue(found, "Test workspace should be found in database");

        System.out.println("[DEBUG_LOG] Workspace creation test completed successfully");
    }
}