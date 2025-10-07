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

/**
 * REST-контроллер AvailabilityController, предоставляющий API BookingTower.
 */
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

    /**
     * Получает список доступных календарных слотов для конкретного рабочего пространства в данную дату.
     * Коды ответа:
     * 200 OK — список слотов.
     * 400 Bad Request — некорректный запрос (ловится IllegalArgumentException).
     * 500 Internal Server Error — иные ошибки
     *
     * @param workspaceId идентификатор рабочего пространства для получения доступных слотов
     * @param date        Дата, на которую можно проверить доступность, отформатированная как дата ISO (yyyy-mm-dd)
     * @param fromTime    Дополнительный параметр для указания начала диапазона времени, отформатированный как время ISO (HH: MM: SS)
     * @param toTime      Дополнительный параметр для указания конца диапазона времени, отформатированный как время ISO (HH: MM: SS)
     * @return Ответ, содержащий список доступных объектов CalendarSlot, если успешно,
     * или соответствующий ответ на ошибку HTTP, если операция не выполняется
     */
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

    /**
     * Получает Long список доступных календарных слотов для указанного рабочего пространства
     * Возвращает: Page<CalendarSlot> — страницу слотов.
     *
     * @param workspaceId the ID of the workspace for which to find available slots
     * @param date        the date for which to retrieve available slots
     * @param fromTime    optional parameter specifying the start of the time range to filter available slots
     * @param toTime      optional parameter specifying the end of the time range to filter available slots
     * @param pageable    the pagination information, including page number and size
     * @return a ResponseEntity containing a paginated list of available calendar slots, or an error response in case of invalid input or server error
     */
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

    /**
     * Получает список доступных слотов календаря для указанного коворкинга в данную дату.
     * Возвращает: List<CalendarSlot> — свободные слоты по ВСЕМ рабочим местам данного коворкинга.
     *
     * @param coworkingId the identifier of the coworking space
     * @param date        the date for which to retrieve available slots, formatted in ISO.DATE
     * @param fromTime    optional parameter, specifies the start time of the desired time range, formatted in ISO.TIME
     * @param toTime      optional parameter, specifies the end time of the desired time range, formatted in ISO.TIME
     * @return a response entity containing a list of {@code CalendarSlot} objects representing available slots,
     * or an appropriate HTTP status code in case of errors
     */
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

    /**
     * Получает сводку доступности для конкретного рабочего пространства в данную дату.
     *
     * @param workspaceId the unique identifier of the workspace for which the summary is to be retrieved
     * @param date        the date for which the availability summary is requested; must be in ISO format (YYYY-MM-DD)
     * @return a ResponseEntity containing the workspace availability summary if found, a 404 status if the workspace is not found,
     * or a 500 status in case of an internal error
     */
    @GetMapping("/workspace/{workspaceId}/summary")
    public ResponseEntity<AvailabilityService.WorkspaceAvailabilitySummary> getWorkspaceAvailabilitySummary(
            @PathVariable Long workspaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        logger.info("Getting availability summary for workspace {} on date {}", workspaceId, date);

        try {
            // Для отдельного рабочего места нужно сначала определить коворкинг
            // Упрощённая версия: в реальном приложении нужен метод для получения сводки по одному рабочему месту
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

    /**
     * Генерирует слоты доступности для указанного рабочего пространства в пределах данного диапазона дат
     * и дополнительное ежедневное время открытия/закрытия и продолжительность слота.
     *
     * @param workspaceId         the ID of the workspace for which slots are to be generated
     * @param startDate           the start date from which slots are to be generated
     * @param endDate             the end date until which slots are to be generated
     * @param dailyOpenTime       the daily opening time of the workspace (optional)
     * @param dailyCloseTime      the daily closing time of the workspace (optional)
     * @param slotDurationMinutes the duration in minutes for each slot; defaults to 60 minutes if not specified
     * @return ResponseEntity containing a success message if slots are generated successfully,
     * or an error message if the request is invalid or an error occurs during slot generation
     */
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

    /**
     * Логирует и вызывает availabilityService.isSlotAvailable(slotId)? свободен ли конкретный слот.
     * @param slotId
     * @return
     */
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