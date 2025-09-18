package org.example.bookingtower.infrastructure.repository;

import org.example.bookingtower.domain.entity.CalendarSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarSlotRepository extends JpaRepository<CalendarSlot, Long> {
    
    @Query("SELECT cs FROM CalendarSlot cs WHERE cs.seat.id = :seatId AND cs.startAt >= :startDate AND cs.endAt <= :endDate ORDER BY cs.startAt")
    List<CalendarSlot> findBySeatIdAndDateRange(@Param("seatId") Long seatId, 
                                               @Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT cs FROM CalendarSlot cs WHERE cs.seat.id IN :seatIds AND cs.startAt >= :startDate AND cs.endAt <= :endDate AND cs.status = 'OPEN' ORDER BY cs.seat.id, cs.startAt")
    List<CalendarSlot> findAvailableSlotsBySeatIdsAndDateRange(@Param("seatIds") List<Long> seatIds,
                                                              @Param("startDate") LocalDateTime startDate,
                                                              @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT cs FROM CalendarSlot cs WHERE cs.seat.workspace.id = :workspaceId AND cs.startAt >= :startDate AND cs.endAt <= :endDate AND cs.status = 'OPEN' ORDER BY cs.seat.code, cs.startAt")
    List<CalendarSlot> findAvailableSlotsByWorkspaceAndDateRange(@Param("workspaceId") Long workspaceId,
                                                                @Param("startDate") LocalDateTime startDate,
                                                                @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT cs FROM CalendarSlot cs WHERE cs.status = 'HELD' AND cs.holdExpiresAt < :now")
    List<CalendarSlot> findExpiredHolds(@Param("now") LocalDateTime now);
    
    @Query("SELECT cs FROM CalendarSlot cs WHERE cs.status = 'HELD' AND cs.holdUserId = :userId AND cs.holdExpiresAt > :now")
    List<CalendarSlot> findActiveHoldsByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    @Query("SELECT cs FROM CalendarSlot cs WHERE cs.seat.id = :seatId AND cs.startAt = :startAt AND cs.endAt = :endAt")
    Optional<CalendarSlot> findBySeatIdAndTimeSlot(@Param("seatId") Long seatId, 
                                                  @Param("startAt") LocalDateTime startAt, 
                                                  @Param("endAt") LocalDateTime endAt);
    
    @Query("SELECT cs FROM CalendarSlot cs WHERE cs.seat.id = :seatId AND ((cs.startAt < :endAt AND cs.endAt > :startAt)) AND cs.status IN ('HELD', 'BOOKED')")
    List<CalendarSlot> findConflictingSlots(@Param("seatId") Long seatId,
                                           @Param("startAt") LocalDateTime startAt,
                                           @Param("endAt") LocalDateTime endAt);
    
    @Modifying
    @Query("UPDATE CalendarSlot cs SET cs.status = 'OPEN', cs.holdUserId = null, cs.holdExpiresAt = null WHERE cs.status = 'HELD' AND cs.holdExpiresAt < :now")
    int releaseExpiredHolds(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(cs) FROM CalendarSlot cs WHERE cs.seat.workspace.id = :workspaceId AND cs.startAt >= :startDate AND cs.endAt <= :endDate AND cs.status = 'OPEN'")
    long countAvailableSlotsByWorkspaceAndDateRange(@Param("workspaceId") Long workspaceId,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT cs FROM CalendarSlot cs WHERE cs.seat.workspace.coworking.id = :coworkingId AND cs.startAt >= :startDate AND cs.endAt <= :endDate AND cs.status = 'OPEN' ORDER BY cs.seat.workspace.name, cs.seat.code, cs.startAt")
    List<CalendarSlot> findAvailableSlotsByCoworkingAndDateRange(@Param("coworkingId") Long coworkingId,
                                                                @Param("startDate") LocalDateTime startDate,
                                                                @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT cs FROM CalendarSlot cs WHERE cs.status = 'BOOKED' AND cs.startAt >= :startDate AND cs.endAt <= :endDate ORDER BY cs.startAt")
    List<CalendarSlot> findBookedSlotsInDateRange(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT cs FROM CalendarSlot cs WHERE cs.seat.id = :seatId AND cs.status = 'BOOKED' AND cs.startAt >= :startDate ORDER BY cs.startAt")
    List<CalendarSlot> findBookedSlotsBySeatFromDate(@Param("seatId") Long seatId, @Param("startDate") LocalDateTime startDate);
    
    boolean existsBySeatIdAndStartAtAndEndAt(Long seatId, LocalDateTime startAt, LocalDateTime endAt);
}