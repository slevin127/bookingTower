package org.example.bookingtower.infrastructure.repository;

import org.example.bookingtower.domain.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    Page<Booking> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<Booking> findByStatus(Booking.BookingStatus status);
    
    Page<Booking> findByStatusOrderByCreatedAtDesc(Booking.BookingStatus status, Pageable pageable);
    
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status = :status ORDER BY b.createdAt DESC")
    List<Booking> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Booking.BookingStatus status);
    
    @Query("SELECT b FROM Booking b WHERE b.seat.workspace.id = :workspaceId ORDER BY b.createdAt DESC")
    List<Booking> findByWorkspaceId(@Param("workspaceId") Long workspaceId);
    
    @Query("SELECT b FROM Booking b WHERE b.seat.workspace.coworking.id = :coworkingId ORDER BY b.createdAt DESC")
    List<Booking> findByCoworkingId(@Param("coworkingId") Long coworkingId);
    
    @Query("SELECT b FROM Booking b WHERE b.slot.startAt >= :startDate AND b.slot.endAt <= :endDate ORDER BY b.slot.startAt")
    List<Booking> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.slot.startAt >= :startDate AND b.slot.endAt <= :endDate ORDER BY b.slot.startAt")
    List<Booking> findByUserIdAndDateRange(@Param("userId") Long userId, 
                                          @Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' AND b.slot.startAt <= :now AND b.slot.endAt > :now")
    List<Booking> findActiveBookings(@Param("now") LocalDateTime now);
    
    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' AND b.slot.startAt > :now ORDER BY b.slot.startAt")
    List<Booking> findUpcomingBookings(@Param("now") LocalDateTime now);
    
    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' AND b.slot.endAt < :cutoffTime AND b.noShowMarkedAt IS NULL")
    List<Booking> findPotentialNoShows(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND b.createdAt < :cutoffTime")
    List<Booking> findExpiredPendingBookings(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId AND b.status = 'CONFIRMED'")
    long countConfirmedBookingsByUser(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.seat.workspace.id = :workspaceId AND b.status = 'CONFIRMED' AND b.slot.startAt >= :startDate AND b.slot.endAt <= :endDate")
    long countConfirmedBookingsByWorkspaceAndDateRange(@Param("workspaceId") Long workspaceId,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(b.totalPrice) FROM Booking b WHERE b.status = 'CONFIRMED' AND b.confirmedAt >= :startDate AND b.confirmedAt <= :endDate")
    BigDecimal getTotalRevenueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(b.totalPrice) FROM Booking b WHERE b.seat.workspace.coworking.id = :coworkingId AND b.status = 'CONFIRMED' AND b.confirmedAt >= :startDate AND b.confirmedAt <= :endDate")
    BigDecimal getTotalRevenueByCoworkingAndDateRange(@Param("coworkingId") Long coworkingId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status IN ('CONFIRMED', 'PENDING') AND b.slot.startAt > :now ORDER BY b.slot.startAt")
    List<Booking> findUserActiveAndUpcomingBookings(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    @Query("SELECT b FROM Booking b WHERE b.slot.id = :slotId")
    Optional<Booking> findBySlotId(@Param("slotId") Long slotId);
    
    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' AND b.slot.startAt BETWEEN :startOfDay AND :endOfDay")
    List<Booking> findConfirmedBookingsForDay(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    Page<Booking> findAllByOrderByCreatedAtDesc(Pageable pageable);
}