package org.example.bookingtower.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

@Entity
@Table(name = "coworkings")
public class Coworking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(nullable = false)
    private String name;
    
    @NotBlank
    @Column(nullable = false)
    private String address;
    
    @NotNull
    @Column(nullable = false)
    private String timezone = "Europe/Moscow";
    
    @NotNull
    @Column(name = "open_from", nullable = false)
    private LocalTime openFrom = LocalTime.of(9, 0);
    
    @NotNull
    @Column(name = "open_to", nullable = false)
    private LocalTime openTo = LocalTime.of(21, 0);
    
    @Column(nullable = false)
    private Boolean active = true;
    
    // Constructors
    public Coworking() {}
    
    public Coworking(String name, String address) {
        this.name = name;
        this.address = address;
    }
    
    public Coworking(String name, String address, String timezone, LocalTime openFrom, LocalTime openTo) {
        this.name = name;
        this.address = address;
        this.timezone = timezone;
        this.openFrom = openFrom;
        this.openTo = openTo;
    }
    
    // Business methods
    public ZoneId getZoneId() {
        return ZoneId.of(timezone);
    }
    
    public boolean isOpenAt(LocalTime time) {
        return !time.isBefore(openFrom) && !time.isAfter(openTo);
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    public LocalTime getOpenFrom() {
        return openFrom;
    }
    
    public void setOpenFrom(LocalTime openFrom) {
        this.openFrom = openFrom;
    }
    
    public LocalTime getOpenTo() {
        return openTo;
    }
    
    public void setOpenTo(LocalTime openTo) {
        this.openTo = openTo;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coworking coworking = (Coworking) o;
        return Objects.equals(id, coworking.id) && Objects.equals(name, coworking.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
    
    @Override
    public String toString() {
        return "Coworking{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", timezone='" + timezone + '\'' +
                ", openFrom=" + openFrom +
                ", openTo=" + openTo +
                ", active=" + active +
                '}';
    }
}