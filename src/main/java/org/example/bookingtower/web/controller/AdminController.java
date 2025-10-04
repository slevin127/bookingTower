package org.example.bookingtower.web.controller;

import org.example.bookingtower.application.service.AvailabilityService;
import org.example.bookingtower.application.service.BookingService;
import org.example.bookingtower.application.service.UserService;
import org.example.bookingtower.domain.entity.*;
import org.example.bookingtower.infrastructure.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Веб-контроллер AdminController для страниц приложения BookingTower.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final WorkspaceRepository workspaceRepository;
    private final CoworkingRepository coworkingRepository;
    private final WorkspaceSeatRepository workspaceSeatRepository;
    private final CalendarSlotRepository calendarSlotRepository;
    private final AvailabilityService availabilityService;
    private final BookingService bookingService;

    @Autowired
    public AdminController(UserRepository userRepository, 
                          BookingRepository bookingRepository,
                          WorkspaceRepository workspaceRepository,
                          CoworkingRepository coworkingRepository,
                          WorkspaceSeatRepository workspaceSeatRepository,
                          CalendarSlotRepository calendarSlotRepository,
                          AvailabilityService availabilityService,
                          BookingService bookingService) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.workspaceRepository = workspaceRepository;
        this.coworkingRepository = coworkingRepository;
        this.workspaceSeatRepository = workspaceSeatRepository;
        this.calendarSlotRepository = calendarSlotRepository;
        this.availabilityService = availabilityService;
        this.bookingService = bookingService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        // Получаем статистику для панели администратора
        long totalUsers = userRepository.count();
        long totalBookings = bookingRepository.count();
        long totalWorkspaces = workspaceRepository.count();

        // Получаем последние бронирования
        Pageable recentBookingsPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Booking> recentBookings = bookingRepository.findAll(recentBookingsPageable);

        // Получаем бронирования за сегодня
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        List<Booking> todayBookings = bookingRepository.findConfirmedBookingsForDay(startOfDay, endOfDay);

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("totalWorkspaces", totalWorkspaces);
        model.addAttribute("recentBookings", recentBookings.getContent());
        model.addAttribute("todayBookings", todayBookings);
        model.addAttribute("todayBookingsCount", todayBookings.size());

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(Model model, 
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = userRepository.findAll(pageable);

        model.addAttribute("users", users);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", users.getTotalPages());

        return "admin/users";
    }

    @GetMapping("/bookings")
    public String bookings(Model model,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Booking> bookings;

        if (status != null && !status.isEmpty()) {
            try {
                Booking.BookingStatus bookingStatus = Booking.BookingStatus.valueOf(status.toUpperCase());
                bookings = bookingRepository.findByStatusOrderByCreatedAtDesc(bookingStatus, pageable);
            } catch (IllegalArgumentException e) {
                bookings = bookingRepository.findAllByOrderByCreatedAtDesc(pageable);
            }
        } else {
            bookings = bookingRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        model.addAttribute("bookings", bookings);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookings.getTotalPages());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", Booking.BookingStatus.values());

        return "admin/bookings";
    }

    @GetMapping("/workspaces")
    public String workspaces(Model model,
                            @RequestParam(required = false) Long coworkingId,
                            @RequestParam(required = false) Boolean active,
                            @RequestParam(required = false) BigDecimal priceFrom) {
        
        // Загрузите все коворкинги для выпадения
        List<Coworking> coworkings = coworkingRepository.findAll();
        
        // Filter workspaces based on parameters
        List<Workspace> workspaces;
        if (coworkingId != null && active != null) {
            workspaces = active ? 
                workspaceRepository.findByCoworkingIdAndActiveTrue(coworkingId) :
                workspaceRepository.findAll().stream()
                    .filter(w -> w.getCoworking().getId().equals(coworkingId) && !w.getActive())
                    .toList();
        } else if (coworkingId != null) {
            workspaces = workspaceRepository.findAll().stream()
                .filter(w -> w.getCoworking().getId().equals(coworkingId))
                .toList();
        } else if (active != null) {
            workspaces = active ? 
                workspaceRepository.findByActiveTrue() :
                workspaceRepository.findAll().stream()
                    .filter(w -> !w.getActive())
                    .toList();
        } else {
            workspaces = workspaceRepository.findAll();
        }
        
        // Apply price filter if specified
        if (priceFrom != null) {
            workspaces = workspaces.stream()
                .filter(w -> w.getPricePerHour().compareTo(priceFrom) >= 0)
                .toList();
        }
        
        // Load seats for each workspace (for detail modals)
        for (Workspace workspace : workspaces) {
            List<WorkspaceSeat> seats = workspaceSeatRepository.findByWorkspaceIdAndActiveTrueOrderByCode(workspace.getId());
            workspace.setSeats(seats);
        }
        
        // Calculate statistics
        long totalWorkspaces = workspaceRepository.count();
        long activeWorkspaces = workspaceRepository.findByActiveTrue().size();
        long totalSeats = workspaceRepository.findAll().stream()
                .mapToLong(w -> w.getSeatsTotal() != null ? w.getSeatsTotal() : 0)
                .sum();
        
        // Calculate available slots (approximate - sum from all workspaces for next week)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextWeek = now.plusDays(7);
        long totalSlots = workspaces.stream()
            .mapToLong(w -> calendarSlotRepository
                .countAvailableSlotsByWorkspaceAndDateRange(w.getId(), now, nextWeek))
            .sum();
        
        model.addAttribute("workspaces", workspaces);
        model.addAttribute("coworkings", coworkings);
        model.addAttribute("totalWorkspaces", totalWorkspaces);
        model.addAttribute("activeWorkspaces", activeWorkspaces);
        model.addAttribute("totalSeats", totalSeats);
        model.addAttribute("totalSlots", totalSlots);
        
        return "admin/workspaces";
    }

    @PostMapping("/workspaces")
    public String createWorkspace(@RequestParam String name,
                                 @RequestParam Long coworkingId,
                                 @RequestParam Integer seatsTotal,
                                 @RequestParam BigDecimal pricePerHour,
                                 @RequestParam(defaultValue = "true") Boolean active,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false) String amenities,
                                 RedirectAttributes redirectAttributes) {
        try {
            Optional<Coworking> coworking = coworkingRepository.findById(coworkingId);
            if (coworking.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Коворкинг не найден");
                return "redirect:/admin/workspaces";
            }
            
            Workspace workspace = new Workspace();
            workspace.setName(name);
            workspace.setCoworking(coworking.get());
            workspace.setSeatsTotal(seatsTotal);
            workspace.setPricePerHour(pricePerHour);
            workspace.setActive(active);
            workspace.setDescription(description);
            workspace.setAmenities(amenities);
            
            workspaceRepository.save(workspace);
            
            // Create seats for the workspace
            createSeatsForWorkspace(workspace);
            
            redirectAttributes.addFlashAttribute("success", "Рабочее место успешно создано");
            return "redirect:/admin/workspaces";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при создании рабочего места: " + e.getMessage());
            return "redirect:/admin/workspaces";
        }
    }
    
    @PostMapping("/workspaces/{workspaceId}/toggle-status")
    public ResponseEntity<String> toggleWorkspaceStatus(@PathVariable Long workspaceId,
                                                       @RequestParam Boolean active) {
        try {
            Optional<Workspace> workspaceOpt = workspaceRepository.findById(workspaceId);
            if (workspaceOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Workspace workspace = workspaceOpt.get();
            workspace.setActive(active);
            workspaceRepository.save(workspace);
            
            // Update all seats status as well
            List<WorkspaceSeat> seats = workspaceSeatRepository.findByWorkspaceIdAndActiveTrue(workspaceId);
            for (WorkspaceSeat seat : seats) {
                seat.setActive(active);
                workspaceSeatRepository.save(seat);
            }
            
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("error");
        }
    }

    @PostMapping("/workspaces/{workspaceId}/generate-slots")
    public String generateSlots(@PathVariable Long workspaceId,
                               @RequestParam LocalDate startDate,
                               @RequestParam LocalDate endDate,
                               @RequestParam(defaultValue = "09:00") String startTime,
                               @RequestParam(defaultValue = "21:00") String endTime,
                               @RequestParam(defaultValue = "60") Integer slotDuration,
                               RedirectAttributes redirectAttributes) {
        try {
            LocalTime start = LocalTime.parse(startTime);
            LocalTime end = LocalTime.parse(endTime);
            availabilityService.generateSlots(workspaceId, startDate, endDate, start, end, slotDuration);
            redirectAttributes.addFlashAttribute("success", "Слоты успешно сгенерированы");
            return "redirect:/admin/workspaces";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при генерации слотов: " + e.getMessage());
            return "redirect:/admin/workspaces";
        }
    }
    
    @GetMapping("/workspaces/{workspaceId}/edit")
    public String editWorkspaceForm(@PathVariable Long workspaceId, Model model) {
        Optional<Workspace> workspaceOpt = workspaceRepository.findById(workspaceId);
        if (workspaceOpt.isEmpty()) {
            return "redirect:/admin/workspaces?error=workspace-not-found";
        }
        
        List<Coworking> coworkings = coworkingRepository.findAll();
        model.addAttribute("workspace", workspaceOpt.get());
        model.addAttribute("coworkings", coworkings);
        return "admin/workspace-edit";
    }
    
    @PostMapping("/workspaces/{workspaceId}/edit")
    public String updateWorkspace(@PathVariable Long workspaceId,
                                 @RequestParam String name,
                                 @RequestParam Long coworkingId,
                                 @RequestParam Integer seatsTotal,
                                 @RequestParam BigDecimal pricePerHour,
                                 @RequestParam Boolean active,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false) String amenities,
                                 RedirectAttributes redirectAttributes) {
        try {
            Optional<Workspace> workspaceOpt = workspaceRepository.findById(workspaceId);
            if (workspaceOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Рабочее место не найдено");
                return "redirect:/admin/workspaces";
            }
            
            Optional<Coworking> coworking = coworkingRepository.findById(coworkingId);
            if (coworking.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Коворкинг не найден");
                return "redirect:/admin/workspaces";
            }
            
            Workspace workspace = workspaceOpt.get();
            Integer oldSeatsTotal = workspace.getSeatsTotal();
            
            workspace.setName(name);
            workspace.setCoworking(coworking.get());
            workspace.setSeatsTotal(seatsTotal);
            workspace.setPricePerHour(pricePerHour);
            workspace.setActive(active);
            workspace.setDescription(description);
            workspace.setAmenities(amenities);
            
            workspaceRepository.save(workspace);
            
            // Обновите места, если Total изменилось
            if (!oldSeatsTotal.equals(seatsTotal)) {
                updateSeatsForWorkspace(workspace, oldSeatsTotal, seatsTotal);
            }
            
            redirectAttributes.addFlashAttribute("success", "Рабочее место успешно обновлено");
            return "redirect:/admin/workspaces";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении рабочего места: " + e.getMessage());
            return "redirect:/admin/workspaces";
        }
    }

    private void createSeatsForWorkspace(Workspace workspace) {
        // Создать места на основе SeatStotal
        String workspaceName = workspace.getName().replaceAll("[^A-Za-z0-9]", "");
        if (workspaceName.length() > 5) {
            workspaceName = workspaceName.substring(0, 5);
        }
        
        for (int i = 1; i <= workspace.getSeatsTotal(); i++) {
            WorkspaceSeat seat = new WorkspaceSeat();
            seat.setWorkspace(workspace);
            seat.setCode(workspaceName.toUpperCase() + "-" + String.format("%02d", i));
            seat.setDescription("Место " + i);
            seat.setActive(true);
            workspaceSeatRepository.save(seat);
        }
    }
    
    private void updateSeatsForWorkspace(Workspace workspace, Integer oldTotal, Integer newTotal) {
        if (newTotal > oldTotal) {
            // Добавить новые места
            String workspaceName = workspace.getName().replaceAll("[^A-Za-z0-9]", "");
            if (workspaceName.length() > 5) {
                workspaceName = workspaceName.substring(0, 5);
            }
            
            for (int i = oldTotal + 1; i <= newTotal; i++) {
                WorkspaceSeat seat = new WorkspaceSeat();
                seat.setWorkspace(workspace);
                seat.setCode(workspaceName.toUpperCase() + "-" + String.format("%02d", i));
                seat.setDescription("Место " + i);
                seat.setActive(true);
                workspaceSeatRepository.save(seat);
            }
        } else if (newTotal < oldTotal) {
            // Удалить лишние сиденья (деактивировать их вместо удаления)
            List<WorkspaceSeat> allSeats = workspaceSeatRepository.findByWorkspaceIdAndActiveTrueOrderByCode(workspace.getId());
            for (int i = newTotal; i < allSeats.size() && i < oldTotal; i++) {
                WorkspaceSeat seat = allSeats.get(i);
                seat.setActive(false);
                workspaceSeatRepository.save(seat);
            }
        }
    }

    @GetMapping("/booking/{bookingId}")
    public String viewBooking(@PathVariable Long bookingId, Model model) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return "redirect:/admin/bookings?error=booking-not-found";
        }

        model.addAttribute("booking", booking);
        return "admin/booking-details";
    }

    @PostMapping("/booking/{bookingId}/cancel")
    public String cancelBooking(@PathVariable Long bookingId,
                               @RequestParam(defaultValue = "Отменено администратором") String reason) {
        try {
            bookingService.adminCancelBooking(bookingId, reason);
            return "redirect:/admin/bookings?success=booking-cancelled";
        } catch (Exception e) {
            return "redirect:/admin/bookings?error=failed-to-cancel";
        }
    }

    @GetMapping("/workspaces/{workspaceId}/slots")
    public String workspaceSlots(@PathVariable Long workspaceId, Model model,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "50") int size,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) LocalDate date) {
        
        Optional<Workspace> workspaceOpt = workspaceRepository.findById(workspaceId);
        if (workspaceOpt.isEmpty()) {
            return "redirect:/admin/workspaces?error=workspace-not-found";
        }

        Workspace workspace = workspaceOpt.get();
        
        // Set default date to today if not provided
        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.atTime(23, 59, 59);
        
        // Get slots for the workspace and date
        List<CalendarSlot> slots;
        if (status != null && !status.isEmpty()) {
            try {
                CalendarSlot.SlotStatus slotStatus = CalendarSlot.SlotStatus.valueOf(status.toUpperCase());
                slots = calendarSlotRepository.findByWorkspaceAndDateRangeAndStatus(workspaceId, startOfDay, endOfDay, slotStatus);
            } catch (IllegalArgumentException e) {
                slots = calendarSlotRepository.findByWorkspaceAndDateRange(workspaceId, startOfDay, endOfDay);
            }
        } else {
            slots = calendarSlotRepository.findByWorkspaceAndDateRange(workspaceId, startOfDay, endOfDay);
        }
        
        model.addAttribute("workspace", workspace);
        model.addAttribute("slots", slots);
        model.addAttribute("targetDate", targetDate);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", CalendarSlot.SlotStatus.values());
        
        return "admin/workspace-slots";
    }

    @PostMapping("/slots/{slotId}/freeze")
    public ResponseEntity<String> freezeSlot(@PathVariable Long slotId) {
        try {
            Optional<CalendarSlot> slotOpt = calendarSlotRepository.findById(slotId);
            if (slotOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            CalendarSlot slot = slotOpt.get();
            slot.freeze();
            calendarSlotRepository.save(slot);
            
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("error: " + e.getMessage());
        }
    }

    @PostMapping("/slots/{slotId}/unfreeze")
    public ResponseEntity<String> unfreezeSlot(@PathVariable Long slotId) {
        try {
            Optional<CalendarSlot> slotOpt = calendarSlotRepository.findById(slotId);
            if (slotOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            CalendarSlot slot = slotOpt.get();
            slot.unfreeze();
            calendarSlotRepository.save(slot);
            
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("error: " + e.getMessage());
        }
    }

    @PostMapping("/slots/{slotId}/toggle-freeze")
    public ResponseEntity<String> toggleSlotFreeze(@PathVariable Long slotId) {
        try {
            Optional<CalendarSlot> slotOpt = calendarSlotRepository.findById(slotId);
            if (slotOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            CalendarSlot slot = slotOpt.get();
            if (slot.isFrozen()) {
                slot.unfreeze();
            } else {
                slot.freeze();
            }
            calendarSlotRepository.save(slot);
            
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("error: " + e.getMessage());
        }
    }

    @PostMapping("/slots/{slotId}/edit")
    public String editSlot(@PathVariable Long slotId,
                          @RequestParam LocalDateTime startAt,
                          @RequestParam LocalDateTime endAt,
                          @RequestParam Long seatId,
                          RedirectAttributes redirectAttributes) {
        try {
            Optional<CalendarSlot> slotOpt = calendarSlotRepository.findById(slotId);
            if (slotOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Слот не найден");
                return "redirect:/admin/workspaces";
            }

            Optional<WorkspaceSeat> seatOpt = workspaceSeatRepository.findById(seatId);
            if (seatOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Место не найдено");
                return "redirect:/admin/workspaces";
            }

            CalendarSlot slot = slotOpt.get();
            WorkspaceSeat seat = seatOpt.get();
            
            // Check for conflicts with the new time slot
            List<CalendarSlot> conflicts = calendarSlotRepository.findConflictingSlots(seatId, startAt, endAt);
            conflicts.removeIf(conflictSlot -> conflictSlot.getId().equals(slotId)); // Exclude current slot
            
            if (!conflicts.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Конфликт с существующим слотом");
                return "redirect:/admin/workspaces/" + seat.getWorkspace().getId() + "/slots";
            }

            slot.setStartAt(startAt);
            slot.setEndAt(endAt);
            slot.setSeat(seat);
            calendarSlotRepository.save(slot);

            redirectAttributes.addFlashAttribute("success", "Слот успешно обновлен");
            return "redirect:/admin/workspaces/" + seat.getWorkspace().getId() + "/slots";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении слота: " + e.getMessage());
            return "redirect:/admin/workspaces";
        }
    }

    @PostMapping("/slots/{slotId}/delete")
    public String deleteSlot(@PathVariable Long slotId, RedirectAttributes redirectAttributes) {
        try {
            Optional<CalendarSlot> slotOpt = calendarSlotRepository.findById(slotId);
            if (slotOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Слот не найден");
                return "redirect:/admin/workspaces";
            }

            CalendarSlot slot = slotOpt.get();
            Long workspaceId = slot.getSeat().getWorkspace().getId();
            
            if (slot.isBooked()) {
                redirectAttributes.addFlashAttribute("error", "Нельзя удалить забронированный слот");
                return "redirect:/admin/workspaces/" + workspaceId + "/slots";
            }

            calendarSlotRepository.delete(slot);
            redirectAttributes.addFlashAttribute("success", "Слот успешно удален");
            return "redirect:/admin/workspaces/" + workspaceId + "/slots";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении слота: " + e.getMessage());
            return "redirect:/admin/workspaces";
        }
    }
}

