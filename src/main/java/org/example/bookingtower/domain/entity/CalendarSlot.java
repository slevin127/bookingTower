package org.example.bookingtower.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Сущность CalendarSlot доменной модели BookingTower.
 */
@Entity
@Table(name = "calendar_slots", 
    indexes = {
        @Index(name = "idx_slot_seat_time", columnList = "seat_id, start_at"),
        @Index(name = "idx_slot_status", columnList = "status"),
        @Index(name = "idx_slot_hold_expires", columnList = "hold_expires_at"),
        @Index(name = "idx_slot_start_at", columnList = "start_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_slot_seat_time_booked", 
                         columnNames = {"seat_id", "start_at", "end_at", "status"})
    }
)
public class CalendarSlot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private WorkspaceSeat seat;
    
    @NotNull
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;
    
    @NotNull
    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;
    
    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    private SlotStatus status = SlotStatus.OPEN;
    
    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;
    
    @Column(name = "hold_user_id")
    private Long holdUserId;
    
    public enum SlotStatus {
        OPEN,    // Available for booking
        HELD,    // Temporarily held by user (10 minutes)
        BOOKED,  // Confirmed booking
        FROZEN   // Temporarily unavailable (admin controlled)
    }
    
    // Конструкторы
    public CalendarSlot() {}
    
    public CalendarSlot(WorkspaceSeat seat, LocalDateTime startAt, LocalDateTime endAt) {
        this.seat = seat;
        this.startAt = startAt;
        this.endAt = endAt;
    }
    
    // Бизнес-методы
    public boolean isAvailable() {
        return status == SlotStatus.OPEN || (status == SlotStatus.HELD && isHoldExpired());
    }
    
    public boolean isHeld() {
        return status == SlotStatus.HELD && !isHoldExpired();
    }
    
    public boolean isBooked() {
        return status == SlotStatus.BOOKED;
    }
    
    public boolean isFrozen() {
        return status == SlotStatus.FROZEN;
    }
    
    public boolean isHoldExpired() {
        return holdExpiresAt != null && LocalDateTime.now().isAfter(holdExpiresAt);
    }
    
    public void hold(Long userId, LocalDateTime expiresAt) {
        if (!isAvailable()) {
            throw new IllegalStateException("Slot is not available for holding");
        }
        this.status = SlotStatus.HELD;
        this.holdUserId = userId;
        this.holdExpiresAt = expiresAt;
    }
    
    public void book() {
        if (status != SlotStatus.HELD) {
            throw new IllegalStateException("Slot must be held before booking");
        }
        this.status = SlotStatus.BOOKED;
        this.holdExpiresAt = null;
    }
    
    public void release() {
        this.status = SlotStatus.OPEN;
        this.holdUserId = null;
        this.holdExpiresAt = null;
    }
    
    public void freeze() {
        if (status == SlotStatus.BOOKED) {
            throw new IllegalStateException("Cannot freeze a booked slot");
        }
        this.status = SlotStatus.FROZEN;
        this.holdUserId = null;
        this.holdExpiresAt = null;
    }
    
    public void unfreeze() {
        if (status == SlotStatus.FROZEN) {
            this.status = SlotStatus.OPEN;
        }
    }
    
    public boolean isHeldBy(Long userId) {
        return status == SlotStatus.HELD && Objects.equals(holdUserId, userId) && !isHoldExpired();
    }
    
    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public WorkspaceSeat getSeat() {
        return seat;
    }
    
    public void setSeat(WorkspaceSeat seat) {
        this.seat = seat;
    }
    
    public LocalDateTime getStartAt() {
        return startAt;
    }
    
    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }
    
    public LocalDateTime getEndAt() {
        return endAt;
    }
    
    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }
    
    public SlotStatus getStatus() {
        return status;
    }
    
    public void setStatus(SlotStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getHoldExpiresAt() {
        return holdExpiresAt;
    }
    
    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }
    
    public Long getHoldUserId() {
        return holdUserId;
    }
    
    public void setHoldUserId(Long holdUserId) {
        this.holdUserId = holdUserId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalendarSlot that = (CalendarSlot) o;
        return Objects.equals(id, that.id) && 
               Objects.equals(seat, that.seat) && 
               Objects.equals(startAt, that.startAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, seat, startAt);
    }
    
    @Override
    public String toString() {
        return "CalendarSlot{" +
                "id=" + id +
                ", startAt=" + startAt +
                ", endAt=" + endAt +
                ", status=" + status +
                ", holdExpiresAt=" + holdExpiresAt +
                '}';
    }
}

