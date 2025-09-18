package org.example.bookingtower.web.controller;

import org.example.bookingtower.application.service.AvailabilityService;
import org.example.bookingtower.application.service.BookingService;
import org.example.bookingtower.application.service.UserService;
import org.example.bookingtower.domain.entity.Booking;
import org.example.bookingtower.domain.entity.User;
import org.example.bookingtower.domain.entity.Workspace;
import org.example.bookingtower.infrastructure.repository.BookingRepository;
import org.example.bookingtower.infrastructure.repository.UserRepository;
import org.example.bookingtower.infrastructure.repository.WorkspaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final WorkspaceRepository workspaceRepository;
    private final AvailabilityService availabilityService;
    private final BookingService bookingService;

    @Autowired
    public AdminController(UserRepository userRepository, 
                          BookingRepository bookingRepository,
                          WorkspaceRepository workspaceRepository,
                          AvailabilityService availabilityService,
                          BookingService bookingService) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.workspaceRepository = workspaceRepository;
        this.availabilityService = availabilityService;
        this.bookingService = bookingService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        // Get statistics for dashboard
        long totalUsers = userRepository.count();
        long totalBookings = bookingRepository.count();
        long totalWorkspaces = workspaceRepository.count();

        // Get recent bookings
        Pageable recentBookingsPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Booking> recentBookings = bookingRepository.findAll(recentBookingsPageable);

        // Get today's bookings
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
    public String workspaces(Model model) {
        List<Workspace> workspaces = workspaceRepository.findAll();
        model.addAttribute("workspaces", workspaces);
        return "admin/workspaces";
    }

    @PostMapping("/workspaces/{workspaceId}/generate-slots")
    public String generateSlots(@PathVariable Long workspaceId,
                               @RequestParam LocalDate startDate,
                               @RequestParam LocalDate endDate) {
        try {
            availabilityService.generateSlots(workspaceId, startDate, endDate, null, null, 60);
            return "redirect:/admin/workspaces?success=slots-generated";
        } catch (Exception e) {
            return "redirect:/admin/workspaces?error=failed-to-generate-slots";
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
}
