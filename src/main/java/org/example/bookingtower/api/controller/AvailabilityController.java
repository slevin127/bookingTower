package org.example.bookingtower.api.controller;

import org.example.bookingtower.application.service.AvailabilityService;
import org.example.bookingtower.domain.entity.CalendarSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/availability")
@CrossOrigin(origins = "*")
public class AvailabilityController {
    
    private static final Logger logger = LoggerFactory.getLogger(AvailabilityController.class);
    
    private final AvailabilityService availabilityService;
    
    @Autowired
    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }
    
    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<CalendarSlot>> getAvailableSlots(
            @PathVariable Long workspaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime fromTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime toTime) {
        
        logger.info("Getting available slots for workspace {} on date {}", workspaceId, date);
        
        try {
            List<CalendarSlot> availableSlots = availabilityService.getAvailableSlots(workspaceId, date, fromTime, toTime);
            return ResponseEntity.ok(availableSlots);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for availability: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error getting available slots", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/workspace/{workspaceId}/paged")
    public ResponseEntity<Page<CalendarSlot>> getAvailableSlotsPaged(
            @PathVariable Long workspaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime fromTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime toTime,
            Pageable pageable) {
        
        logger.info("Getting paged available slots for workspace {} on date {}", workspaceId, date);
        
        try {
            Page<CalendarSlot> availableSlots = availabilityService.getAvailableSlots(workspaceId, date, fromTime, toTime, pageable);
            return ResponseEntity.ok(availableSlots);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for availability: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error getting available slots", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/coworking/{coworkingId}")
    public ResponseEntity<List<CalendarSlot>> getAvailableSlotsByCoworking(
            @PathVariable Long coworkingId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime fromTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime toTime) {
        
        logger.info("Getting available slots for coworking {} on date {}", coworkingId, date);
        
        try {
            List<CalendarSlot> availableSlots = availabilityService.getAvailableSlotsByCoworking(coworkingId, date, fromTime, toTime);
            return ResponseEntity.ok(availableSlots);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for coworking availability: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error getting available slots for coworking", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/workspace/{workspaceId}/summary")
    public ResponseEntity<AvailabilityService.WorkspaceAvailabilitySummary> getWorkspaceAvailabilitySummary(
            @PathVariable Long workspaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        logger.info("Getting availability summary for workspace {} on date {}", workspaceId, date);
        
        try {
            // For single workspace, we need to get its coworking first
            // This is a simplified version - in a real implementation, you'd have a method to get single workspace summary
            List<AvailabilityService.WorkspaceAvailabilitySummary> summaries = 
                availabilityService.getWorkspaceAvailabilitySummary(1L, date); // Assuming coworking ID 1
            
            AvailabilityService.WorkspaceAvailabilitySummary summary = summaries.stream()
                .filter(s -> s.getWorkspaceId().equals(workspaceId))
                .findFirst()
                .orElse(null);
                
            if (summary != null) {
                return ResponseEntity.ok(summary);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error getting workspace availability summary", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/workspace/{workspaceId}/generate-slots")
    public ResponseEntity<String> generateSlots(
            @PathVariable Long workspaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime dailyOpenTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime dailyCloseTime,
            @RequestParam(defaultValue = "60") int slotDurationMinutes) {
        
        logger.info("Generating slots for workspace {} from {} to {}", workspaceId, startDate, endDate);
        
        try {
            availabilityService.generateSlots(workspaceId, startDate, endDate, dailyOpenTime, dailyCloseTime, slotDurationMinutes);
            return ResponseEntity.ok("Slots generated successfully");
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for slot generation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error generating slots", e);
            return ResponseEntity.internalServerError().body("Failed to generate slots");
        }
    }
    
    @GetMapping("/slot/{slotId}/available")
    public ResponseEntity<Boolean> isSlotAvailable(@PathVariable Long slotId) {
        logger.info("Checking if slot {} is available", slotId);
        
        try {
            boolean available = availabilityService.isSlotAvailable(slotId);
            return ResponseEntity.ok(available);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid slot ID: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error checking slot availability", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}