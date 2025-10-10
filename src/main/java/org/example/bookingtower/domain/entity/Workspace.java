package org.example.bookingtower.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Сущность Workspace доменной модели BookingTower.
 */
@Entity
@Table(name = "workspaces", indexes = {
    @Index(name = "idx_workspace_coworking", columnList = "coworking_id"),
    @Index(name = "idx_workspace_active", columnList = "active")
})
public class Workspace {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coworking_id", nullable = false)
    private Coworking coworking;
    
    @NotBlank
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @NotNull
    @PositiveOrZero
    @Column(name = "seats_total", nullable = false)
    private Integer seatsTotal;
    
    @Column(columnDefinition = "TEXT")
    private String amenities;
    
    @NotNull
    @Column(name = "price_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;
    
    @Column(nullable = false)
    private Boolean active = true;
    
  // Временное поле для рабочих мест (не сохраняется, используется для пользовательского интерфейса)
    @Transient
    private List<WorkspaceSeat> seats;
    
    // Конструкторы
    public Workspace() {}
    
    public Workspace(Coworking coworking, String name, Integer seatsTotal, BigDecimal pricePerHour) {
        this.coworking = coworking;
        this.name = name;
        this.seatsTotal = seatsTotal;
        this.pricePerHour = pricePerHour;
    }
    
    public Workspace(Coworking coworking, String name, String description, Integer seatsTotal, 
                    String amenities, BigDecimal pricePerHour) {
        this.coworking = coworking;
        this.name = name;
        this.description = description;
        this.seatsTotal = seatsTotal;
        this.amenities = amenities;
        this.pricePerHour = pricePerHour;
    }
    
    // Бизнес-методы
    public boolean isActive() {
        return active != null && active && coworking != null && coworking.getActive();
    }
    
    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Coworking getCoworking() {
        return coworking;
    }
    
    public void setCoworking(Coworking coworking) {
        this.coworking = coworking;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getSeatsTotal() {
        return seatsTotal;
    }
    
    public void setSeatsTotal(Integer seatsTotal) {
        this.seatsTotal = seatsTotal;
    }
    
    public String getAmenities() {
        return amenities;
    }
    
    public void setAmenities(String amenities) {
        this.amenities = amenities;
    }
    
    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }
    
    public void setPricePerHour(BigDecimal pricePerHour) {
        this.pricePerHour = pricePerHour;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    public List<WorkspaceSeat> getSeats() {
        return seats;
    }
    
    public void setSeats(List<WorkspaceSeat> seats) {
        this.seats = seats;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Workspace workspace = (Workspace) o;
        return Objects.equals(id, workspace.id) && Objects.equals(name, workspace.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
    
    @Override
    public String toString() {
        return "Workspace{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", seatsTotal=" + seatsTotal +
                ", pricePerHour=" + pricePerHour +
                ", active=" + active +
                '}';
    }
}

