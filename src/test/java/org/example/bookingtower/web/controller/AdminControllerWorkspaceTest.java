package org.example.bookingtower.web.controller;

import org.example.bookingtower.application.service.AvailabilityService;
import org.example.bookingtower.application.service.BookingService;
import org.example.bookingtower.domain.entity.Coworking;
import org.example.bookingtower.domain.entity.Workspace;
import org.example.bookingtower.infrastructure.repository.BookingRepository;
import org.example.bookingtower.infrastructure.repository.CalendarSlotRepository;
import org.example.bookingtower.infrastructure.repository.CoworkingRepository;
import org.example.bookingtower.infrastructure.repository.UserRepository;
import org.example.bookingtower.infrastructure.repository.WorkspaceRepository;
import org.example.bookingtower.infrastructure.repository.WorkspaceSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AdminControllerWorkspaceTest {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private CoworkingRepository coworkingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private WorkspaceSeatRepository workspaceSeatRepository;

    @Autowired
    private CalendarSlotRepository calendarSlotRepository;

    private AdminController adminController;
    private Coworking testCoworking;

    @BeforeEach
    public void setUp() {
        System.out.println("[DEBUG_LOG] Setting up test data");

        // Создайте экземпляр Admincontroller
        adminController = new AdminController(userRepository, bookingRepository, workspaceRepository, 
                                            coworkingRepository, workspaceSeatRepository, calendarSlotRepository, 
                                            availabilityService, bookingService);

        // Создать тест -коворкинг
        testCoworking = new Coworking();
        testCoworking.setName("Test Coworking");
        testCoworking.setAddress("Test Address");
        testCoworking.setTimezone("Europe/Moscow");
        testCoworking.setOpenFrom(LocalTime.of(9, 0));
        testCoworking.setOpenTo(LocalTime.of(21, 0));
        testCoworking.setActive(true);

        testCoworking = coworkingRepository.save(testCoworking);
        System.out.println("[DEBUG_LOG] Created test coworking with ID: " + testCoworking.getId());
    }

    @Test
    public void testCreateWorkspaceViaController() {
        System.out.println("[DEBUG_LOG] Testing workspace creation via controller method");

        // Считайте рабочие места перед созданием
        long initialCount = workspaceRepository.count();
        System.out.println("[DEBUG_LOG] Initial workspace count: " + initialCount);

        // Создайте REDIERECTATTRIBUTES MOCK
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        // Вызовите метод контроллера напрямую
        String result = adminController.createWorkspace(
                "Test Workspace Controller",
                testCoworking.getId(),
                15,
                new BigDecimal("750.50"),
                true,
                "Test Description Controller",
                "Wi-Fi, Coffee, Printer",
                redirectAttributes
        );

        // Проверьте перенаправление
        assertEquals("redirect:/admin/workspaces", result);

        // Проверить рабочее пространство было создано
        long finalCount = workspaceRepository.count();
        System.out.println("[DEBUG_LOG] Final workspace count: " + finalCount);
        assertEquals(initialCount + 1, finalCount, "Workspace count should increase by 1");

        // Найдите созданную рабочую область
        List<Workspace> workspaces = workspaceRepository.findAll();
        Workspace createdWorkspace = workspaces.stream()
                .filter(w -> "Test Workspace Controller".equals(w.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(createdWorkspace, "Created workspace should be found");
        assertEquals("Test Workspace Controller", createdWorkspace.getName());
        assertEquals("Test Description Controller", createdWorkspace.getDescription());
        assertEquals(Integer.valueOf(15), createdWorkspace.getSeatsTotal());
        assertEquals("Wi-Fi, Coffee, Printer", createdWorkspace.getAmenities());
        assertEquals(new BigDecimal("750.50"), createdWorkspace.getPricePerHour());
        assertTrue(createdWorkspace.getActive());
        assertNotNull(createdWorkspace.getCoworking());
        assertEquals(testCoworking.getId(), createdWorkspace.getCoworking().getId());

        System.out.println("[DEBUG_LOG] Workspace creation via controller test completed successfully");
    }

    @Test
    public void testCreateWorkspaceWithInvalidCoworkingId() {
        System.out.println("[DEBUG_LOG] Testing workspace creation with invalid coworking ID");

        // Считайте рабочие места перед попыткой создания
        long initialCount = workspaceRepository.count();
        System.out.println("[DEBUG_LOG] Initial workspace count: " + initialCount);

        // Создайте REDIERECTATTRIBUTES MOCK
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        // Вызовите метод контроллера с неверным идентификатором коворкинга
        String result = adminController.createWorkspace(
                "Test Workspace Invalid",
                99999L, // Invalid ID
                10,
                new BigDecimal("500.00"),
                true,
                "Test Description",
                "Wi-Fi",
                redirectAttributes
        );

        // Проверьте перенаправление
        assertEquals("redirect:/admin/workspaces", result);

        // Проверить рабочее пространство не было создано
        long finalCount = workspaceRepository.count();
        System.out.println("[DEBUG_LOG] Final workspace count: " + finalCount);
        assertEquals(initialCount, finalCount, "Workspace count should remain the same");

        System.out.println("[DEBUG_LOG] Invalid coworking ID test completed successfully");
    }
}
