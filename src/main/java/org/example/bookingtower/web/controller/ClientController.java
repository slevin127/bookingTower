package org.example.bookingtower.web.controller;

import org.example.bookingtower.application.service.AvailabilityService;
import org.example.bookingtower.application.service.BookingService;
import org.example.bookingtower.config.CustomUserDetailsService;
import org.example.bookingtower.domain.entity.Booking;
import org.example.bookingtower.domain.entity.CalendarSlot;
import org.example.bookingtower.domain.entity.User;
import org.example.bookingtower.domain.entity.Workspace;
import org.example.bookingtower.infrastructure.repository.WorkspaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Веб-контроллер ClientController для страниц приложения BookingTower.
 */
@Controller
@RequestMapping("/client")
public class ClientController {

    private final BookingService bookingService;
    private final AvailabilityService availabilityService;
    private final WorkspaceRepository workspaceRepository;

    @Autowired
    public ClientController(BookingService bookingService,
                            AvailabilityService availabilityService,
                            WorkspaceRepository workspaceRepository) {
        this.bookingService = bookingService;
        this.availabilityService = availabilityService;
        this.workspaceRepository = workspaceRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        CustomUserDetailsService.CustomUserPrincipal principal =
                (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        // Получаем недавние бронирования пользователя
        Pageable recentBookingsPageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        Page<Booking> recentBookings = bookingService.getUserBookings(user.getId(), recentBookingsPageable);

        // Получаем предстоящие бронирования пользователя
        List<Booking> upcomingBookings = bookingService.getUserBookings(user.getId()).stream()
                .filter(booking -> booking.getSlot().getStartAt().isAfter(java.time.LocalDateTime.now()))
                .filter(booking -> booking.getStatus() == Booking.BookingStatus.CONFIRMED ||
                        booking.getStatus() == Booking.BookingStatus.PENDING)
                .limit(5)
                .toList();

        model.addAttribute("user", user);
        model.addAttribute("recentBookings", recentBookings.getContent());
        model.addAttribute("upcomingBookings", upcomingBookings);
        model.addAttribute("totalBookings", recentBookings.getTotalElements());

        return "client/dashboard";
    }

    @GetMapping("/bookings")
    public String bookings(Model model, Authentication authentication,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size) {
        CustomUserDetailsService.CustomUserPrincipal principal =
                (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Booking> bookings = bookingService.getUserBookings(user.getId(), pageable);

        model.addAttribute("bookings", bookings);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookings.getTotalPages());

        return "client/bookings";
    }

    @GetMapping("/book")
    public String bookWorkspace(Model model,
                                @RequestParam(required = false) Long workspaceId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Workspace> workspaces = workspaceRepository.findAll();
        model.addAttribute("workspaces", workspaces);

        if (workspaceId != null && date != null) {
            try {
                List<CalendarSlot> availableSlots = availabilityService.getAvailableSlots(workspaceId, date, null, null);
                model.addAttribute("availableSlots", availableSlots);
                model.addAttribute("selectedWorkspaceId", workspaceId);
                model.addAttribute("selectedDate", date);

                // Получаем информацию о рабочем месте
                Workspace selectedWorkspace = workspaceRepository.findById(workspaceId).orElse(null);
                model.addAttribute("selectedWorkspace", selectedWorkspace);
            } catch (Exception e) {
                model.addAttribute("error", "Ошибка при загрузке доступных слотов");
            }
        }

        return "client/book";
    }

    @PostMapping("/book/hold")
    public String holdSlot(@RequestParam Long slotId, Authentication authentication) {
        CustomUserDetailsService.CustomUserPrincipal principal =
                (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        try {
            String holdToken = bookingService.holdSlot(user.getId(), slotId);
            return "redirect:/client/book/confirm?slotId=" + slotId + "&holdToken=" + holdToken;
        } catch (Exception e) {
            return "redirect:/client/book?error=failed-to-hold-slot";
        }
    }

    @GetMapping("/book/confirm")
    public String confirmBooking(@RequestParam Long slotId,
                                 @RequestParam String holdToken,
                                 Model model,
                                 Authentication authentication) {
        CustomUserDetailsService.CustomUserPrincipal principal =
                (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        try {
            BigDecimal price = bookingService.calculatePrice(slotId);
            model.addAttribute("slotId", slotId);
            model.addAttribute("holdToken", holdToken);
            model.addAttribute("price", price);
            model.addAttribute("user", user);

            return "client/confirm-booking";
        } catch (Exception e) {
            return "redirect:/client/book?error=invalid-slot";
        }
    }

    /**
     * Обрабатывает запрос на подтверждение бронирования путем подтверждения слота и данных пользователя,
     * Расчет цены и создание подтвержденного бронирования.
     *
     * @param slotId         the ID of the slot to be booked
     * @param holdToken      the token used to hold the slot temporarily
     * @param authentication the authentication object containing user details
     * @return a redirect URL indicating whether the booking was successfully confirmed or an error occurred
     */
    @PostMapping("/book/confirm")
    public String processBooking(@RequestParam Long slotId,
                                 @RequestParam String holdToken,
                                 Authentication authentication) {
        CustomUserDetailsService.CustomUserPrincipal principal =
                (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        try {
            BigDecimal price = bookingService.calculatePrice(slotId);
            Booking booking = bookingService.confirmBooking(user.getId(), slotId, price);
            return "redirect:/client/booking/" + booking.getId() + "?success=booking-confirmed";
        } catch (Exception e) {
            return "redirect:/client/book?error=failed-to-confirm-booking";
        }
    }

    @GetMapping("/booking/{bookingId}")
    public String viewBooking(@PathVariable Long bookingId, Model model, Authentication authentication) {
        CustomUserDetailsService.CustomUserPrincipal principal =
                (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        Booking booking = bookingService.getBooking(bookingId, user.getId()).orElse(null);
        if (booking == null) {
            return "redirect:/client/bookings?error=booking-not-found";
        }

        model.addAttribute("booking", booking);
        return "client/booking-details";
    }

    @PostMapping("/booking/{bookingId}/cancel")
    public String cancelBooking(@PathVariable Long bookingId,
                                @RequestParam(defaultValue = "Отменено пользователем") String reason,
                                Authentication authentication) {
        CustomUserDetailsService.CustomUserPrincipal principal =
                (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        try {
            bookingService.cancelBooking(bookingId, user.getId(), reason);
            return "redirect:/client/bookings?success=booking-cancelled";
        } catch (Exception e) {
            return "redirect:/client/bookings?error=failed-to-cancel";
        }
    }
}