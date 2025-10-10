package org.example.bookingtower.web.controller;

import org.example.bookingtower.application.service.AvailabilityService;
import org.example.bookingtower.application.service.BookingService;
import org.example.bookingtower.application.service.WorkspaceService;
import org.example.bookingtower.config.CustomUserDetailsService;
import org.example.bookingtower.domain.entity.Booking;
import org.example.bookingtower.domain.entity.CalendarSlot;
import org.example.bookingtower.domain.entity.User;
import org.example.bookingtower.domain.entity.Workspace;
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
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Веб-контроллер ClientController для страниц приложения BookingTower.
 */
@Controller
@RequestMapping("/client")
public class ClientController {

    private final BookingService bookingService;
    private final AvailabilityService availabilityService;
    private final WorkspaceService workspaceService;
    private static final Logger logger = LoggerFactory.getLogger(AvailabilityService.class);

    @Autowired
    public ClientController(BookingService bookingService,
                            AvailabilityService availabilityService,
                            WorkspaceService workspaceService) {
        this.bookingService = bookingService;
        this.availabilityService = availabilityService;
        this.workspaceService = workspaceService;
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
                                @RequestParam(required = false) Long workspaceId,
                                @RequestParam(defaultValue = "09:00") String startTime,
                                @RequestParam(defaultValue = "21:00") String endTime,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        // Загружаем все рабочие места
        List<Workspace> workspaces = workspaceService.findAll();
        model.addAttribute("workspaces", workspaces);

        // Проверка наличия workspaceId и даты
        if (workspaceId != null && date != null) {
            try {
                // Проверяем корректность времени
                LocalTime start = LocalTime.parse(startTime);
                LocalTime end = LocalTime.parse(endTime);
                if (start.isAfter(end)) {
                    logger.warn("Некорректный временной интервал: startTime={} позже endTime={}", start, end);

                    model.addAttribute("error", "Время начала не может быть позже времени окончания.");
                    return "client/book";
                }

                // Получаем доступные слоты
                List<CalendarSlot> availableSlots = availabilityService.getAvailableSlots(workspaceId, date, start, end);
                logger.debug("Слоты, возвращенные getAvailableSlots: {}", availableSlots);
                logger.info("Доступные слоты: {}", availableSlots == null ? "null" : availableSlots.size());

                if (availableSlots == null || availableSlots.isEmpty()) {
                    model.addAttribute("info", "Нет доступных слотов для выбранного рабочего места и времени.");
                } else {
                    model.addAttribute("availableSlots", availableSlots);
                }

                // Загружаем выбранное рабочее место
                Workspace selectedWorkspace = workspaceService.findById(workspaceId);
                if (selectedWorkspace == null) {
                    model.addAttribute("error", "Рабочее место не найдено.");
                    return "client/book";
                }
                model.addAttribute("selectedWorkspace", selectedWorkspace);

                // Добавляем параметры пользователя
                model.addAttribute("selectedWorkspaceId", workspaceId);
                model.addAttribute("selectedDate", date);

            } catch (DateTimeParseException e) {
                model.addAttribute("error", "Неверный формат времени начала или окончания.");
            } catch (NoSuchElementException e) {
                model.addAttribute("error", "Рабочее место не найдено.");
            } catch (Exception e) {

                model.addAttribute("error", "Ошибка при загрузке доступных слотов.");
            }
        } else {
            if (workspaceId == null) {
                model.addAttribute("info", "Выберите рабочее место.");
            }
            if (date == null) {
                model.addAttribute("info", "Выберите дату.");
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