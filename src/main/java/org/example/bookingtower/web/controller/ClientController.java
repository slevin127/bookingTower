package org.example.bookingtower.web.controller;

import org.example.bookingtower.application.service.AvailabilityService;
import org.example.bookingtower.application.service.BookingService;
import org.example.bookingtower.application.service.CoworkingService;
import org.example.bookingtower.application.service.WorkspaceService;
import org.example.bookingtower.config.CustomUserDetailsService;
import org.example.bookingtower.domain.entity.Booking;
import org.example.bookingtower.domain.entity.CalendarSlot;
import org.example.bookingtower.domain.entity.Coworking;
import org.example.bookingtower.domain.entity.User;
import org.example.bookingtower.domain.entity.Workspace;
import org.example.bookingtower.domain.entity.WorkspaceSeat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Веб-контроллер ClientController для страниц приложения BookingTower.
 */
@Controller
@RequestMapping("/client")
public class ClientController {

    private final BookingService bookingService;
    private final AvailabilityService availabilityService;
    private final WorkspaceService workspaceService;
    private final CoworkingService coworkingService;
    private static final Logger logger = LoggerFactory.getLogger(AvailabilityService.class);

    @Autowired
    public ClientController(BookingService bookingService,
                            AvailabilityService availabilityService,
                            WorkspaceService workspaceService,
                            CoworkingService coworkingService) {
        this.bookingService = bookingService;
        this.availabilityService = availabilityService;
        this.workspaceService = workspaceService;
        this.coworkingService = coworkingService;
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

    /**
     * Обрабатывает запрос на бронирование рабочего места.
     * @param model
     * @param workspaceId
     * @param date
     * @return
     */
    @GetMapping("/book")
    public String bookWorkspace(Model model,
                                @RequestParam(required = false) Long coworkingId,
                                @RequestParam(required = false) Long workspaceId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        model.addAttribute("pageTitle", "Забронировать место");

        List<Coworking> coworkings = coworkingService.findAllActive();
        model.addAttribute("coworkings", coworkings);
        model.addAttribute("selectedCoworkingId", coworkingId);

        LocalDate selectedDate = date != null ? date : LocalDate.now();
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("hasScheduleData", false);

        if (coworkingId == null) {
            if (coworkings.isEmpty()) {
                model.addAttribute("info", "Нет доступных коворкингов для бронирования.");
            }
            return "client/book";
        }

        try {
            Coworking selectedCoworking = coworkingService.getActiveById(coworkingId);
            model.addAttribute("selectedCoworking", selectedCoworking);

            List<Workspace> coworkingWorkspaces = workspaceService.findActiveByCoworking(coworkingId);
            model.addAttribute("coworkingWorkspaces", coworkingWorkspaces);

            if (coworkingWorkspaces.isEmpty()) {
                model.addAttribute("info", "В выбранном коворкинге нет активных рабочих пространств.");
                return "client/book";
            }

            if (workspaceId == null) {
                return "client/book";
            }

            Workspace selectedWorkspace = workspaceService.findById(workspaceId);
            if (selectedWorkspace == null || !Boolean.TRUE.equals(selectedWorkspace.getActive()) ||
                    !selectedWorkspace.getCoworking().getId().equals(coworkingId)) {
                model.addAttribute("error", "Выбранное рабочее пространство недоступно.");
                return "client/book";
            }

            model.addAttribute("selectedWorkspace", selectedWorkspace);
            model.addAttribute("selectedWorkspaceId", workspaceId);

            List<WorkspaceSeat> workspaceSeats = workspaceService.findActiveSeatsByWorkspace(workspaceId);
            model.addAttribute("workspaceSeats", workspaceSeats);

            if (workspaceSeats.isEmpty()) {
                model.addAttribute("info", "Для выбранного пространства нет активных рабочих мест.");
                return "client/book";
            }

            List<CalendarSlot> scheduleSlots = availabilityService.getWorkspaceSchedule(workspaceId, selectedDate);

            String slotTimePattern = "HH:mm";
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(slotTimePattern);

            List<LocalTime> timeSlots = scheduleSlots.stream()
                    .map(slot -> slot.getStartAt().toLocalTime())
                    .distinct()
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toList());

            if (timeSlots.isEmpty()) {
                timeSlots = buildTimeSlots(selectedCoworking.getOpenFrom(), selectedCoworking.getOpenTo());
            }

            model.addAttribute("timeSlots", timeSlots);
            model.addAttribute("hasScheduleData", !scheduleSlots.isEmpty());
            model.addAttribute("slotTimePattern", slotTimePattern);

            Map<Long, Map<String, CalendarSlot>> slotMatrix = workspaceSeats.stream()
                    .collect(Collectors.toMap(
                            WorkspaceSeat::getId,
                            seat -> new LinkedHashMap<>(),
                            (existing, replacement) -> existing,
                            LinkedHashMap::new));

            scheduleSlots.forEach(slot -> {
                Long seatId = slot.getSeat().getId();
                String timeKey = slot.getStartAt().toLocalTime().format(timeFormatter);
                slotMatrix.computeIfAbsent(seatId, id -> new LinkedHashMap<>())
                        .put(timeKey, slot);
            });

            model.addAttribute("slotMatrix", slotMatrix);

        } catch (NoSuchElementException e) {
            model.addAttribute("error", e.getMessage());
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        } catch (Exception e) {
            logger.error("Ошибка при загрузке расписания", e);
            model.addAttribute("error", "Ошибка при загрузке расписания рабочего пространства.");
        }

        return "client/book";
    }


    @PostMapping("/book/hold")
    public String holdSlot(@RequestParam Long slotId,
                           @RequestParam(required = false) Long coworkingId,
                           @RequestParam(required = false) Long workspaceId,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           Authentication authentication) {
        CustomUserDetailsService.CustomUserPrincipal principal =
                (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        try {
            String holdToken = bookingService.holdSlot(user.getId(), slotId);
            return "redirect:/client/book/confirm?slotId=" + slotId + "&holdToken=" + holdToken;
        } catch (Exception e) {
            StringBuilder redirect = new StringBuilder("redirect:/client/book?error=failed-to-hold-slot");
            if (coworkingId != null) {
                redirect.append("&coworkingId=").append(coworkingId);
            }
            if (workspaceId != null) {
                redirect.append("&workspaceId=").append(workspaceId);
            }
            if (date != null) {
                redirect.append("&date=").append(date);
            }
            return redirect.toString();
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

    private List<LocalTime> buildTimeSlots(LocalTime openFrom, LocalTime openTo) {
        List<LocalTime> slots = new ArrayList<>();
        if (openFrom == null || openTo == null) {
            return slots;
        }

        LocalTime current = openFrom;
        while (current.isBefore(openTo)) {
            slots.add(current);
            current = current.plusHours(1);
        }

        return slots;
    }
}