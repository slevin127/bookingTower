package org.example.bookingtower.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_booking", columnList = "booking_id"),
    @Index(name = "idx_payment_external", columnList = "external_id", unique = true),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_provider", columnList = "provider"),
    @Index(name = "idx_payment_created", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
    
    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    private PaymentProvider provider = PaymentProvider.YOOKASSA;
    
    @NotBlank
    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;
    
    @NotNull
    @Positive
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @NotNull
    @Column(nullable = false, length = 3)
    private String currency = "RUB";
    
    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.NEW;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "captured_at")
    private LocalDateTime capturedAt;
    
    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;
    
    @Column(name = "payment_url", length = 1000)
    private String paymentUrl;
    
    @Column(name = "confirmation_token")
    private String confirmationToken;
    
    @Column(name = "idempotency_key")
    private String idempotencyKey;
    
    @Column(name = "failure_reason")
    private String failureReason;
    
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    public enum PaymentProvider {
        YOOKASSA
    }
    
    public enum PaymentStatus {
        NEW,        // Payment created but not initiated
        PENDING,    // Payment initiated, waiting for user action
        SUCCEEDED,  // Payment completed successfully
        CANCELED,   // Payment canceled
        REFUNDED    // Payment refunded (partially or fully)
    }
    
    // Constructors
    public Payment() {}
    
    public Payment(Booking booking, String externalId, BigDecimal amount, String idempotencyKey) {
        this.booking = booking;
        this.externalId = externalId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
    }
    
    // Business methods
    public boolean canBeRefunded() {
        return status == PaymentStatus.SUCCEEDED && 
               (refundAmount == null || refundAmount.compareTo(amount) < 0);
    }
    
    public BigDecimal getAvailableRefundAmount() {
        if (refundAmount == null) {
            return amount;
        }
        return amount.subtract(refundAmount);
    }
    
    public boolean isFullyRefunded() {
        return refundAmount != null && refundAmount.compareTo(amount) >= 0;
    }
    
    public void markAsSucceeded() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be marked as succeeded");
        }
        this.status = PaymentStatus.SUCCEEDED;
        this.capturedAt = LocalDateTime.now();
    }
    
    public void markAsCanceled(String reason) {
        if (status == PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("Succeeded payments cannot be canceled");
        }
        this.status = PaymentStatus.CANCELED;
        this.failureReason = reason;
    }
    
    public void addRefund(BigDecimal refundAmountToAdd) {
        if (!canBeRefunded()) {
            throw new IllegalStateException("Payment cannot be refunded");
        }
        
        BigDecimal newRefundAmount = (refundAmount != null ? refundAmount : BigDecimal.ZERO)
                .add(refundAmountToAdd);
        
        if (newRefundAmount.compareTo(amount) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed payment amount");
        }
        
        this.refundAmount = newRefundAmount;
        
        if (isFullyRefunded()) {
            this.status = PaymentStatus.REFUNDED;
        }
    }
    
    public void markAsPending(String paymentUrl, String confirmationToken) {
        if (status != PaymentStatus.NEW) {
            throw new IllegalStateException("Only new payments can be marked as pending");
        }
        this.status = PaymentStatus.PENDING;
        this.paymentUrl = paymentUrl;
        this.confirmationToken = confirmationToken;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Booking getBooking() {
        return booking;
    }
    
    public void setBooking(Booking booking) {
        this.booking = booking;
    }
    
    public PaymentProvider getProvider() {
        return provider;
    }
    
    public void setProvider(PaymentProvider provider) {
        this.provider = provider;
    }
    
    public String getExternalId() {
        return externalId;
    }
    
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public PaymentStatus getStatus() {
        return status;
    }
    
    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }
    
    public void setCapturedAt(LocalDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }
    
    public BigDecimal getRefundAmount() {
        return refundAmount;
    }
    
    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }
    
    public String getPaymentUrl() {
        return paymentUrl;
    }
    
    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }
    
    public String getConfirmationToken() {
        return confirmationToken;
    }
    
    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }
    
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
    
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(id, payment.id) && Objects.equals(externalId, payment.externalId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, externalId);
    }
    
    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", provider=" + provider +
                ", externalId='" + externalId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}