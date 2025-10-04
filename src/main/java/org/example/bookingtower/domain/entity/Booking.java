package org.example.bookingtower.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Сущность Booking доменной модели BookingTower.
 */
@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_booking_user", columnList = "user_id"),
    @Index(name = "idx_booking_seat", columnList = "seat_id"),
    @Index(name = "idx_booking_slot", columnList = "slot_id"),
    @Index(name = "idx_booking_status", columnList = "status"),
    @Index(name = "idx_booking_created", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class Booking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private WorkspaceSeat seat;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private CalendarSlot slot;
    
    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;
    
    @NotNull
    @Positive
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
    
    @NotNull
    @Column(nullable = false, length = 3)
    private String currency = "RUB";
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
    
    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;
    
    @Column(name = "cancellation_reason")
    private String cancellationReason;
    
    @Column(name = "no_show_marked_at")
    private LocalDateTime noShowMarkedAt;
    
    public enum BookingStatus {
        PENDING,    // Waiting for payment
        CONFIRMED,  // Payment successful, booking confirmed
        CANCELED,   // Booking canceled
        NO_SHOW     // User didn't show up
    }
    
    // Конструкторы
    public Booking() {}
    
    public Booking(User user, WorkspaceSeat seat, CalendarSlot slot, BigDecimal totalPrice) {
        this.user = user;
        this.seat = seat;
        this.slot = slot;
        this.totalPrice = totalPrice;
    }
    
    // Бизнес-методы
    public boolean canBeCanceled() {
        return status == BookingStatus.CONFIRMED || status == BookingStatus.PENDING;
    }
    
    public boolean canBeMarkedAsNoShow() {
        return status == BookingStatus.CONFIRMED && 
               slot != null && 
               LocalDateTime.now().isAfter(slot.getStartAt());
    }
    
    public void confirm() {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException("Only pending bookings can be confirmed");
        }
        this.status = BookingStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }
    
    public void cancel(String reason) {
        if (!canBeCanceled()) {
            throw new IllegalStateException("Booking cannot be canceled in current status: " + status);
        }
        this.status = BookingStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
        this.cancellationReason = reason;
    }
    
    public void markAsNoShow() {
        if (!canBeMarkedAsNoShow()) {
            throw new IllegalStateException("Booking cannot be marked as no-show");
        }
        this.status = BookingStatus.NO_SHOW;
        this.noShowMarkedAt = LocalDateTime.now();
    }
    
    public boolean isActive() {
        return status == BookingStatus.CONFIRMED;
    }
    
    public LocalDateTime getBookingStartTime() {
        return slot != null ? slot.getStartAt() : null;
    }
    
    public LocalDateTime getBookingEndTime() {
        return slot != null ? slot.getEndAt() : null;
    }
    
    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public WorkspaceSeat getSeat() {
        return seat;
    }
    
    public void setSeat(WorkspaceSeat seat) {
        this.seat = seat;
    }
    
    public CalendarSlot getSlot() {
        return slot;
    }
    
    public void setSlot(CalendarSlot slot) {
        this.slot = slot;
    }
    
    public BookingStatus getStatus() {
        return status;
    }
    
    public void setStatus(BookingStatus status) {
        this.status = status;
    }
    
    public BigDecimal getTotalPrice() {
        return totalPrice;
    }
    
    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }
    
    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }
    
    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }
    
    public void setCanceledAt(LocalDateTime canceledAt) {
        this.canceledAt = canceledAt;
    }
    
    public String getCancellationReason() {
        return cancellationReason;
    }
    
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
    
    public LocalDateTime getNoShowMarkedAt() {
        return noShowMarkedAt;
    }
    
    public void setNoShowMarkedAt(LocalDateTime noShowMarkedAt) {
        this.noShowMarkedAt = noShowMarkedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        return Objects.equals(id, booking.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Booking{" +
                "id=" + id +
                ", status=" + status +
                ", totalPrice=" + totalPrice +
                ", currency='" + currency + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

