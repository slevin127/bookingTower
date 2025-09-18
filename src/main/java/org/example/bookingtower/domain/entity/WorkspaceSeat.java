package org.example.bookingtower.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

@Entity
@Table(name = "workspace_seats", indexes = {
    @Index(name = "idx_seat_workspace", columnList = "workspace_id"),
    @Index(name = "idx_seat_code", columnList = "code"),
    @Index(name = "idx_seat_active", columnList = "active")
})
public class WorkspaceSeat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;
    
    @NotBlank
    @Column(nullable = false)
    private String code;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    // Constructors
    public WorkspaceSeat() {}
    
    public WorkspaceSeat(Workspace workspace, String code) {
        this.workspace = workspace;
        this.code = code;
    }
    
    public WorkspaceSeat(Workspace workspace, String code, String description) {
        this.workspace = workspace;
        this.code = code;
        this.description = description;
    }
    
    // Business methods
    public boolean isActive() {
        return active != null && active && workspace != null && workspace.isActive();
    }
    
    public String getFullCode() {
        if (workspace != null && workspace.getName() != null) {
            return workspace.getName() + "-" + code;
        }
        return code;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Workspace getWorkspace() {
        return workspace;
    }
    
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
        WorkspaceSeat that = (WorkspaceSeat) o;
        return Objects.equals(id, that.id) && Objects.equals(code, that.code);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, code);
    }
    
    @Override
    public String toString() {
        return "WorkspaceSeat{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", active=" + active +
                '}';
    }
}