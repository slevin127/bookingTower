package org.example.bookingtower.application.service;

import org.example.bookingtower.domain.entity.*;
import org.example.bookingtower.infrastructure.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Сервис BookingService, инкапсулирующий бизнес-логику BookingTower.
 */
@Service
@Transactional
public class BookingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);
    
    private final BookingRepository bookingRepository;
    private final CalendarSlotRepository calendarSlotRepository;
    private final WorkspaceSeatRepository workspaceSeatRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    @Value("${app.booking.hold-duration-minutes:10}")
    private int holdDurationMinutes;
    
    @Value("${app.booking.cancellation-hours-before:2}")
    private int cancellationHoursBefore;
    
    @Autowired
    public BookingService(BookingRepository bookingRepository,
                         CalendarSlotRepository calendarSlotRepository,
                         WorkspaceSeatRepository workspaceSeatRepository,
                         UserRepository userRepository,
                         EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.calendarSlotRepository = calendarSlotRepository;
        this.workspaceSeatRepository = workspaceSeatRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }
    
    public String holdSlot(Long userId, Long slotId) {
        logger.info("Holding slot {} for user {}", slotId, userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        CalendarSlot slot = calendarSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        
        if (!slot.isAvailable()) {
            throw new IllegalStateException("Slot is not available for holding");
        }
        
        // Проверяем, есть ли у пользователя активные удержания
        List<CalendarSlot> activeHolds = calendarSlotRepository.findActiveHoldsByUser(userId, LocalDateTime.now());
        if (activeHolds.size() >= 3) { // Limit to 3 concurrent holds
            throw new IllegalStateException("User has too many active holds");
        }
        
        LocalDateTime holdExpiresAt = LocalDateTime.now().plusMinutes(holdDurationMinutes);
        slot.hold(userId, holdExpiresAt);
        
        calendarSlotRepository.save(slot);
        
        String holdId = "HOLD_" + slotId + "_" + userId + "_" + System.currentTimeMillis();
        logger.info("Slot {} held successfully for user {} until {}", slotId, userId, holdExpiresAt);
        
        return holdId;
    }
    
    public Booking confirmBooking(Long userId, Long slotId, BigDecimal totalPrice) {
        logger.info("Confirming booking for slot {} by user {}", slotId, userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        CalendarSlot slot = calendarSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        
        if (!slot.isHeldBy(userId)) {
            throw new IllegalStateException("Slot is not held by this user");
        }
        
        WorkspaceSeat seat = slot.getSeat();
        if (!seat.isActive()) {
            throw new IllegalStateException("Seat is not active");
        }
        
        // Create booking
        Booking booking = new Booking(user, seat, slot, totalPrice);
        booking.setCreatedAt(LocalDateTime.now());
        
        Booking savedBooking = bookingRepository.save(booking);
        
        // Update slot status to booked
        slot.book();
        calendarSlotRepository.save(slot);
        
        logger.info("Booking {} created successfully", savedBooking.getId());
        
        // Send confirmation email
        try {
            String bookingDetails = formatBookingDetails(savedBooking);
            emailService.sendBookingConfirmation(user.getEmail(), bookingDetails);
        } catch (Exception e) {
            logger.error("Failed to send booking confirmation email", e);
        }
        
        return savedBooking;
    }
    
    public void cancelBooking(Long bookingId, Long userId, String reason) {
        logger.info("Canceling booking {} by user {}", bookingId, userId);
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        
        if (!booking.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("User is not authorized to cancel this booking");
        }
        
        if (!booking.canBeCanceled()) {
            throw new IllegalStateException("Booking cannot be canceled");
        }
        
        // Проверяем правила отмены
        LocalDateTime bookingStart = booking.getBookingStartTime();
        LocalDateTime cancellationDeadline = bookingStart.minusHours(cancellationHoursBefore);
        
        if (LocalDateTime.now().isAfter(cancellationDeadline)) {
            throw new IllegalStateException("Booking cannot be canceled less than " + cancellationHoursBefore + " hours before start time");
        }
        
        booking.cancel(reason);
        bookingRepository.save(booking);
        
        // Release the slot
        CalendarSlot slot = booking.getSlot();
        slot.release();
        calendarSlotRepository.save(slot);
        
        logger.info("Booking {} canceled successfully", bookingId);
        
        // Send cancellation email
        try {
            String bookingDetails = formatBookingDetails(booking);
            emailService.sendBookingCancellation(booking.getUser().getEmail(), bookingDetails, reason);
        } catch (Exception e) {
            logger.error("Failed to send booking cancellation email", e);
        }
    }
    
    public void markAsNoShow(Long bookingId) {
        logger.info("Marking booking {} as no-show", bookingId);
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        
        if (!booking.canBeMarkedAsNoShow()) {
            throw new IllegalStateException("Booking cannot be marked as no-show");
        }
        
        booking.markAsNoShow();
        bookingRepository.save(booking);
        
        logger.info("Booking {} marked as no-show", bookingId);
    }
    
    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    public Page<Booking> getUserBookings(Long userId, Pageable pageable) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    public Optional<Booking> getBooking(Long bookingId, Long userId) {
        Optional<Booking> booking = bookingRepository.findById(bookingId);
        if (booking.isPresent() && !booking.get().getUser().getId().equals(userId)) {
            return Optional.empty(); // User can only see their own bookings
        }
        return booking;
    }
    
    public List<Booking> getUpcomingBookings() {
        return bookingRepository.findUpcomingBookings(LocalDateTime.now());
    }
    
    public List<Booking> getActiveBookings() {
        return bookingRepository.findActiveBookings(LocalDateTime.now());
    }
    
    public List<Booking> getPotentialNoShows() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30); // 30 minutes after booking end
        return bookingRepository.findPotentialNoShows(cutoffTime);
    }
    
    public void releaseExpiredHolds() {
        logger.info("Releasing expired holds");
        int releasedCount = calendarSlotRepository.releaseExpiredHolds(LocalDateTime.now());
        logger.info("Released {} expired holds", releasedCount);
    }
    
    public BigDecimal calculatePrice(Long slotId) {
        CalendarSlot slot = calendarSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        
        WorkspaceSeat seat = slot.getSeat();
        return seat.getWorkspace().getPricePerHour();
    }
    
    public List<Booking> getBookingsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return bookingRepository.findByDateRange(startDate, endDate);
    }
    
    public List<Booking> getBookingsByWorkspace(Long workspaceId) {
        return bookingRepository.findByWorkspaceId(workspaceId);
    }
    
    public List<Booking> getBookingsByCoworking(Long coworkingId) {
        return bookingRepository.findByCoworkingId(coworkingId);
    }
    
    public BigDecimal getTotalRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal revenue = bookingRepository.getTotalRevenueByDateRange(startDate, endDate);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
    
    public long getBookingCount(Long workspaceId, LocalDateTime startDate, LocalDateTime endDate) {
        return bookingRepository.countConfirmedBookingsByWorkspaceAndDateRange(workspaceId, startDate, endDate);
    }
    
    private String formatBookingDetails(Booking booking) {
        return String.format(
            "Бронирование #%d\n" +
            "Место: %s\n" +
            "Время: %s - %s\n" +
            "Стоимость: %.2f RUB\n" +
            "Статус: %s",
            booking.getId(),
            booking.getSeat().getFullCode(),
            booking.getBookingStartTime(),
            booking.getBookingEndTime(),
            booking.getTotalPrice(),
            booking.getStatus()
        );
    }
    
    // Admin methods
    public Page<Booking> getAllBookings(Pageable pageable) {
        return bookingRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    public void adminCancelBooking(Long bookingId, String reason) {
        logger.info("Admin canceling booking {}", bookingId);
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        
        if (!booking.canBeCanceled()) {
            throw new IllegalStateException("Booking cannot be canceled");
        }
        
        booking.cancel(reason);
        bookingRepository.save(booking);
        
        // Release the slot
        CalendarSlot slot = booking.getSlot();
        slot.release();
        calendarSlotRepository.save(slot);
        
        logger.info("Booking {} canceled by admin", bookingId);
        
        // Send cancellation email
        try {
            String bookingDetails = formatBookingDetails(booking);
            emailService.sendBookingCancellation(booking.getUser().getEmail(), bookingDetails, reason);
        } catch (Exception e) {
            logger.error("Failed to send booking cancellation email", e);
        }
    }
}

