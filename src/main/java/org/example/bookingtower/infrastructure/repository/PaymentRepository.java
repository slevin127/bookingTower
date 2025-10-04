package org.example.bookingtower.infrastructure.repository;

import org.example.bookingtower.domain.entity.Payment;
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

/**
 * Репозиторий PaymentRepository для доступа к данным BookingTower.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByExternalId(String externalId);
    
    List<Payment> findByBookingId(Long bookingId);
    
    Optional<Payment> findByBookingIdAndStatus(Long bookingId, Payment.PaymentStatus status);
    
    List<Payment> findByStatus(Payment.PaymentStatus status);
    
    Page<Payment> findByStatusOrderByCreatedAtDesc(Payment.PaymentStatus status, Pageable pageable);
    
    @Query("SELECT p FROM Payment p WHERE p.booking.user.id = :userId ORDER BY p.createdAt DESC")
    List<Payment> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT p FROM Payment p WHERE p.booking.user.id = :userId ORDER BY p.createdAt DESC")
    Page<Payment> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT p FROM Payment p WHERE p.idempotencyKey = :key")
    Optional<Payment> findByIdempotencyKey(@Param("key") String idempotencyKey);
    
    @Query("SELECT p FROM Payment p WHERE p.status = 'SUCCEEDED' AND p.capturedAt >= :startDate AND p.capturedAt <= :endDate")
    List<Payment> findSuccessfulPaymentsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT p FROM Payment p WHERE p.booking.seat.workspace.coworking.id = :coworkingId AND p.status = 'SUCCEEDED' AND p.capturedAt >= :startDate AND p.capturedAt <= :endDate")
    List<Payment> findSuccessfulPaymentsByCoworkingAndDateRange(@Param("coworkingId") Long coworkingId,
                                                               @Param("startDate") LocalDateTime startDate,
                                                               @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'SUCCEEDED' AND p.capturedAt >= :startDate AND p.capturedAt <= :endDate")
    BigDecimal getTotalSuccessfulPaymentsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(p.refundAmount) FROM Payment p WHERE p.status = 'REFUNDED' AND p.updatedAt >= :startDate AND p.updatedAt <= :endDate")
    BigDecimal getTotalRefundsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :cutoffTime")
    List<Payment> findExpiredPendingPayments(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'SUCCEEDED' AND p.capturedAt >= :startDate AND p.capturedAt <= :endDate")
    long countSuccessfulPaymentsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.booking.user.id = :userId AND p.status = 'SUCCEEDED'")
    long countSuccessfulPaymentsByUser(@Param("userId") Long userId);
    
    @Query("SELECT p FROM Payment p WHERE p.status IN ('SUCCEEDED', 'REFUNDED') AND p.refundAmount < p.amount ORDER BY p.capturedAt DESC")
    List<Payment> findRefundablePayments();
    
    @Query("SELECT p FROM Payment p WHERE p.provider = :provider AND p.status = :status ORDER BY p.createdAt DESC")
    List<Payment> findByProviderAndStatus(@Param("provider") Payment.PaymentProvider provider, @Param("status") Payment.PaymentStatus status);
    
    @Query("SELECT p FROM Payment p WHERE p.booking.id = :bookingId AND p.status = 'SUCCEEDED' ORDER BY p.capturedAt DESC")
    List<Payment> findSuccessfulPaymentsByBookingId(@Param("bookingId") Long bookingId);
    
    @Query("SELECT AVG(p.amount) FROM Payment p WHERE p.status = 'SUCCEEDED' AND p.capturedAt >= :startDate AND p.capturedAt <= :endDate")
    BigDecimal getAveragePaymentAmountByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    boolean existsByExternalId(String externalId);
    
    boolean existsByIdempotencyKey(String idempotencyKey);
    
    Page<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
