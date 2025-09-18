package org.example.bookingtower.application.service;

import org.example.bookingtower.domain.entity.*;
import org.example.bookingtower.infrastructure.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AvailabilityService {
    
    private static final Logger logger = LoggerFactory.getLogger(AvailabilityService.class);
    
    private final CalendarSlotRepository calendarSlotRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceSeatRepository workspaceSeatRepository;
    private final CoworkingRepository coworkingRepository;
    
    @Value("${app.coworking.timezone:Europe/Moscow}")
    private String defaultTimezone;
    
    @Value("${app.coworking.default-open-from:09:00}")
    private String defaultOpenFrom;
    
    @Value("${app.coworking.default-open-to:21:00}")
    private String defaultOpenTo;
    
    @Autowired
    public AvailabilityService(CalendarSlotRepository calendarSlotRepository,
                              WorkspaceRepository workspaceRepository,
                              WorkspaceSeatRepository workspaceSeatRepository,
                              CoworkingRepository coworkingRepository) {
        this.calendarSlotRepository = calendarSlotRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceSeatRepository = workspaceSeatRepository;
        this.coworkingRepository = coworkingRepository;
    }
    
    public List<CalendarSlot> getAvailableSlots(Long workspaceId, LocalDate date, LocalTime fromTime, LocalTime toTime) {
        logger.info("Getting available slots for workspace {} on {} from {} to {}", workspaceId, date, fromTime, toTime);
        
        Workspace workspace = workspaceRepository.findByIdAndActiveTrue(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found or inactive"));
        
        LocalDateTime startDateTime = date.atTime(fromTime != null ? fromTime : LocalTime.parse(defaultOpenFrom));
        LocalDateTime endDateTime = date.atTime(toTime != null ? toTime : LocalTime.parse(defaultOpenTo));
        
        // Validate time range against coworking hours
        Coworking coworking = workspace.getCoworking();
        if (!coworking.isOpenAt(startDateTime.toLocalTime()) || !coworking.isOpenAt(endDateTime.toLocalTime())) {
            throw new IllegalArgumentException("Requested time is outside coworking operating hours");
        }
        
        List<CalendarSlot> availableSlots = calendarSlotRepository.findAvailableSlotsByWorkspaceAndDateRange(
                workspaceId, startDateTime, endDateTime);
        
        logger.info("Found {} available slots", availableSlots.size());
        return availableSlots;
    }
    
    public Page<CalendarSlot> getAvailableSlots(Long workspaceId, LocalDate date, LocalTime fromTime, LocalTime toTime, Pageable pageable) {
        List<CalendarSlot> allSlots = getAvailableSlots(workspaceId, date, fromTime, toTime);
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allSlots.size());
        
        List<CalendarSlot> pageContent = allSlots.subList(start, end);
        return new PageImpl<>(pageContent, pageable, allSlots.size());
    }
    
    public List<CalendarSlot> getAvailableSlotsByCoworking(Long coworkingId, LocalDate date, LocalTime fromTime, LocalTime toTime) {
        logger.info("Getting available slots for coworking {} on {} from {} to {}", coworkingId, date, fromTime, toTime);
        
        Coworking coworking = coworkingRepository.findByIdAndActiveTrue(coworkingId)
                .orElseThrow(() -> new IllegalArgumentException("Coworking not found or inactive"));
        
        LocalDateTime startDateTime = date.atTime(fromTime != null ? fromTime : coworking.getOpenFrom());
        LocalDateTime endDateTime = date.atTime(toTime != null ? toTime : coworking.getOpenTo());
        
        List<CalendarSlot> availableSlots = calendarSlotRepository.findAvailableSlotsByCoworkingAndDateRange(
                coworkingId, startDateTime, endDateTime);
        
        logger.info("Found {} available slots for coworking", availableSlots.size());
        return availableSlots;
    }
    
    public void generateSlots(Long workspaceId, LocalDate startDate, LocalDate endDate, LocalTime dailyOpenTime, LocalTime dailyCloseTime, int slotDurationMinutes) {
        logger.info("Generating slots for workspace {} from {} to {}", workspaceId, startDate, endDate);
        
        Workspace workspace = workspaceRepository.findByIdAndActiveTrue(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found or inactive"));
        
        List<WorkspaceSeat> seats = workspaceSeatRepository.findByWorkspaceIdAndActiveTrue(workspaceId);
        if (seats.isEmpty()) {
            throw new IllegalArgumentException("No active seats found for workspace");
        }
        
        LocalTime openTime = dailyOpenTime != null ? dailyOpenTime : workspace.getCoworking().getOpenFrom();
        LocalTime closeTime = dailyCloseTime != null ? dailyCloseTime : workspace.getCoworking().getOpenTo();
        
        List<CalendarSlot> slotsToCreate = new ArrayList<>();
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            // Skip weekends if needed (can be made configurable)
            if (date.getDayOfWeek().getValue() > 5) { // Saturday = 6, Sunday = 7
                continue;
            }
            
            for (WorkspaceSeat seat : seats) {
                LocalDateTime currentSlotStart = date.atTime(openTime);
                LocalDateTime dayEnd = date.atTime(closeTime);
                
                while (currentSlotStart.plusMinutes(slotDurationMinutes).isBefore(dayEnd) || 
                       currentSlotStart.plusMinutes(slotDurationMinutes).equals(dayEnd)) {
                    
                    LocalDateTime slotEnd = currentSlotStart.plusMinutes(slotDurationMinutes);
                    
                    // Check if slot already exists
                    if (!calendarSlotRepository.existsBySeatIdAndStartAtAndEndAt(seat.getId(), currentSlotStart, slotEnd)) {
                        CalendarSlot slot = new CalendarSlot(seat, currentSlotStart, slotEnd);
                        slotsToCreate.add(slot);
                    }
                    
                    currentSlotStart = slotEnd;
                }
            }
        }
        
        if (!slotsToCreate.isEmpty()) {
            calendarSlotRepository.saveAll(slotsToCreate);
            logger.info("Generated {} new slots for workspace {}", slotsToCreate.size(), workspaceId);
        } else {
            logger.info("No new slots needed for workspace {}", workspaceId);
        }
    }
    
    public void generateSlotsForAllWorkspaces(LocalDate startDate, LocalDate endDate) {
        logger.info("Generating slots for all active workspaces from {} to {}", startDate, endDate);
        
        List<Workspace> activeWorkspaces = workspaceRepository.findByActiveTrue();
        
        for (Workspace workspace : activeWorkspaces) {
            try {
                generateSlots(workspace.getId(), startDate, endDate, null, null, 60); // 1-hour slots
            } catch (Exception e) {
                logger.error("Failed to generate slots for workspace {}", workspace.getId(), e);
            }
        }
        
        logger.info("Completed slot generation for {} workspaces", activeWorkspaces.size());
    }
    
    public long getAvailableSlotCount(Long workspaceId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        
        return calendarSlotRepository.countAvailableSlotsByWorkspaceAndDateRange(workspaceId, startOfDay, endOfDay);
    }
    
    public List<CalendarSlot> getSlotsBySeat(Long seatId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        
        return calendarSlotRepository.findBySeatIdAndDateRange(seatId, startOfDay, endOfDay);
    }
    
    public List<CalendarSlot> getBookedSlots(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        return calendarSlotRepository.findBookedSlotsInDateRange(startDateTime, endDateTime);
    }
    
    public boolean isSlotAvailable(Long slotId) {
        CalendarSlot slot = calendarSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        
        return slot.isAvailable();
    }
    
    public List<CalendarSlot> findConflictingSlots(Long seatId, LocalDateTime startAt, LocalDateTime endAt) {
        return calendarSlotRepository.findConflictingSlots(seatId, startAt, endAt);
    }
    
    public List<WorkspaceAvailabilitySummary> getWorkspaceAvailabilitySummary(Long coworkingId, LocalDate date) {
        List<Workspace> workspaces = workspaceRepository.findByCoworkingIdAndActiveTrue(coworkingId);
        
        return workspaces.stream()
                .map(workspace -> {
                    long totalSlots = getTotalSlotsForWorkspace(workspace.getId(), date);
                    long availableSlots = getAvailableSlotCount(workspace.getId(), date);
                    long bookedSlots = totalSlots - availableSlots;
                    
                    return new WorkspaceAvailabilitySummary(
                            workspace.getId(),
                            workspace.getName(),
                            totalSlots,
                            availableSlots,
                            bookedSlots,
                            totalSlots > 0 ? (double) availableSlots / totalSlots * 100 : 0
                    );
                })
                .collect(Collectors.toList());
    }
    
    private long getTotalSlotsForWorkspace(Long workspaceId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        
        List<WorkspaceSeat> seats = workspaceSeatRepository.findByWorkspaceIdAndActiveTrue(workspaceId);
        if (seats.isEmpty()) {
            return 0;
        }
        
        List<Long> seatIds = seats.stream().map(WorkspaceSeat::getId).collect(Collectors.toList());
        List<CalendarSlot> allSlots = calendarSlotRepository.findAvailableSlotsBySeatIdsAndDateRange(seatIds, startOfDay, endOfDay);
        
        return allSlots.size();
    }
    
    // Inner class for availability summary
    public static class WorkspaceAvailabilitySummary {
        private final Long workspaceId;
        private final String workspaceName;
        private final long totalSlots;
        private final long availableSlots;
        private final long bookedSlots;
        private final double availabilityPercentage;
        
        public WorkspaceAvailabilitySummary(Long workspaceId, String workspaceName, long totalSlots, 
                                          long availableSlots, long bookedSlots, double availabilityPercentage) {
            this.workspaceId = workspaceId;
            this.workspaceName = workspaceName;
            this.totalSlots = totalSlots;
            this.availableSlots = availableSlots;
            this.bookedSlots = bookedSlots;
            this.availabilityPercentage = availabilityPercentage;
        }
        
        // Getters
        public Long getWorkspaceId() { return workspaceId; }
        public String getWorkspaceName() { return workspaceName; }
        public long getTotalSlots() { return totalSlots; }
        public long getAvailableSlots() { return availableSlots; }
        public long getBookedSlots() { return bookedSlots; }
        public double getAvailabilityPercentage() { return availabilityPercentage; }
    }
}