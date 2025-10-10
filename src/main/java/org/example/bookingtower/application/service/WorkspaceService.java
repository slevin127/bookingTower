package org.example.bookingtower.application.service;

import org.example.bookingtower.domain.entity.Workspace;
import org.example.bookingtower.domain.entity.WorkspaceSeat;
import org.example.bookingtower.domain.entity.Coworking;
import org.example.bookingtower.infrastructure.repository.WorkspaceRepository;
import org.example.bookingtower.infrastructure.repository.WorkspaceSeatRepository;
import org.example.bookingtower.infrastructure.repository.CoworkingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления рабочими пространствами и их местами.
 */
@Service
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceSeatRepository workspaceSeatRepository;
    private final CoworkingRepository coworkingRepository; // Добавлен репозиторий для загрузки Coworking

    @Autowired
    public WorkspaceService(WorkspaceRepository workspaceRepository, WorkspaceSeatRepository workspaceSeatRepository, CoworkingRepository coworkingRepository) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceSeatRepository = workspaceSeatRepository;
        this.coworkingRepository = coworkingRepository;
    }

    /**
     * Создаёт рабочее пространство вместе с местами (WorkspaceSeat).
     *
     * @param name        название рабочего пространства
     * @param seatsTotal  количество мест в рабочем пространстве
     * @param pricePerHour стоимость аренды рабочего места в час
     * @param coworkingId ID коворкинга, к которому относится рабочее пространство
     * @param active      активность рабочего пространства
     * @param description описание рабочего пространства
     * @param amenities   удобства рабочего пространства
     * @return созданное рабочее пространство
     */
    @Transactional
    public Workspace createWorkspaceWithSeats(String name, Integer seatsTotal, BigDecimal pricePerHour, 
                                            Long coworkingId, Boolean active, String description, String amenities) {
        // Загружаем объект Coworking по его ID
        Optional<Coworking> coworkingOptional = coworkingRepository.findById(coworkingId);
        if (coworkingOptional.isEmpty()) {
            throw new IllegalArgumentException("Коворкинг с ID " + coworkingId + " не найден.");
        }
        Coworking coworking = coworkingOptional.get();

        // Создаем объект рабочего пространства
        Workspace workspace = new Workspace();
        workspace.setName(name);
        workspace.setSeatsTotal(seatsTotal);
        workspace.setPricePerHour(pricePerHour);
        workspace.setActive(active != null ? active : true);
        workspace.setDescription(description);
        workspace.setAmenities(amenities);
        workspace.setCoworking(coworking);

        // Сохраняем рабочее пространство
        Workspace savedWorkspace = workspaceRepository.save(workspace);

        // Создаём места для рабочего пространства
        createSeatsForWorkspace(savedWorkspace);

        return savedWorkspace;
    }

    /**
     * Обновляет рабочее пространство.
     *
     * @param workspaceId  ID рабочего пространства
     * @param name         новое название рабочего пространства
     * @param coworkingId  новый ID коворкинга
     * @param seatsTotal   новое количество мест
     * @param pricePerHour новая стоимость аренды в час
     * @param active       новый статус активности
     * @param description  новое описание
     * @param amenities    новые удобства
     * @return обновленное рабочее пространство
     */
    @Transactional
    public Workspace updateWorkspace(Long workspaceId, String name, Long coworkingId, Integer seatsTotal, 
                                   BigDecimal pricePerHour, Boolean active, String description, String amenities) {
        Optional<Workspace> workspaceOpt = workspaceRepository.findById(workspaceId);
        if (workspaceOpt.isEmpty()) {
            throw new IllegalArgumentException("Рабочее место не найдено");
        }
        
        Optional<Coworking> coworking = coworkingRepository.findById(coworkingId);
        if (coworking.isEmpty()) {
            throw new IllegalArgumentException("Коворкинг не найден");
        }
        
        Workspace workspace = workspaceOpt.get();
        Integer oldSeatsTotal = workspace.getSeatsTotal();
        
        workspace.setName(name);
        workspace.setCoworking(coworking.get());
        workspace.setSeatsTotal(seatsTotal);
        workspace.setPricePerHour(pricePerHour);
        workspace.setActive(active);
        workspace.setDescription(description);
        workspace.setAmenities(amenities);
        
        workspaceRepository.save(workspace);
        
        // Обновите места, если Total изменилось
        if (!oldSeatsTotal.equals(seatsTotal)) {
            updateSeatsForWorkspace(workspace, oldSeatsTotal, seatsTotal);
        }
        
        return workspace;
    }

    /**
     * Переключает статус активности рабочего пространства и всех его мест.
     *
     * @param workspaceId ID рабочего пространства
     * @param active      новый статус активности
     * @return обновленное рабочее пространство
     */
    @Transactional
    public Workspace toggleWorkspaceStatus(Long workspaceId, Boolean active) {
        Optional<Workspace> workspaceOpt = workspaceRepository.findById(workspaceId);
        if (workspaceOpt.isEmpty()) {
            throw new IllegalArgumentException("Рабочее место не найдено");
        }
        
        Workspace workspace = workspaceOpt.get();
        workspace.setActive(active);
        workspaceRepository.save(workspace);
        
        // Обновляем статус всех мест
        List<WorkspaceSeat> seats = workspaceSeatRepository.findByWorkspaceIdAndActiveTrue(workspaceId);
        for (WorkspaceSeat seat : seats) {
            seat.setActive(active);
            workspaceSeatRepository.save(seat);
        }
        
        return workspace;
    }

    /**
     * Получает все рабочие пространства.
     *
     * @return список всех рабочих пространств
     */
    public List<Workspace> findAll() {
        return workspaceRepository.findAll();
    }

    /**
     * Возвращает активные рабочие пространства конкретного коворкинга.
     */
    public List<Workspace> findActiveByCoworking(Long coworkingId) {
        return workspaceRepository.findByCoworkingIdAndActiveTrueOrderByName(coworkingId);
    }

    /**
     * Возвращает активные рабочие места (посадки) рабочего пространства, отсортированные по коду.
     */
    public List<WorkspaceSeat> findActiveSeatsByWorkspace(Long workspaceId) {
        return workspaceSeatRepository.findByWorkspaceIdAndActiveTrueOrderByCode(workspaceId);
    }

    /**
     * Получает рабочее пространство по ID.
     *
     * @param workspaceId ID рабочего пространства
     * @return рабочее пространство или null, если не найдено
     */
    public Workspace findById(Long workspaceId) {
        return workspaceRepository.findById(workspaceId).orElse(null);
    }

    /**
     * Создаёт места для рабочего пространства.
     *
     * @param workspace рабочее пространство
     */
    private void createSeatsForWorkspace(Workspace workspace) {
        String workspaceName = workspace.getName().replaceAll("[^A-Za-z0-9]", "");
        if (workspaceName.length() > 5) {
            workspaceName = workspaceName.substring(0, 5);
        }
        
        for (int i = 1; i <= workspace.getSeatsTotal(); i++) {
            WorkspaceSeat seat = new WorkspaceSeat();
            seat.setWorkspace(workspace);
            seat.setCode(workspaceName.toUpperCase() + "-" + String.format("%02d", i));
            seat.setDescription("Место " + i);
            seat.setActive(true);
            workspaceSeatRepository.save(seat);
        }
    }
    
    /**
     * Обновляет места для рабочего пространства при изменении их количества.
     *
     * @param workspace рабочее пространство
     * @param oldTotal  старое количество мест
     * @param newTotal  новое количество мест
     */
    private void updateSeatsForWorkspace(Workspace workspace, Integer oldTotal, Integer newTotal) {
        if (newTotal > oldTotal) {
            // Добавить новые места
            String workspaceName = workspace.getName().replaceAll("[^A-Za-z0-9]", "");
            if (workspaceName.length() > 5) {
                workspaceName = workspaceName.substring(0, 5);
            }
            
            for (int i = oldTotal + 1; i <= newTotal; i++) {
                WorkspaceSeat seat = new WorkspaceSeat();
                seat.setWorkspace(workspace);
                seat.setCode(workspaceName.toUpperCase() + "-" + String.format("%02d", i));
                seat.setDescription("Место " + i);
                seat.setActive(true);
                workspaceSeatRepository.save(seat);
            }
        } else if (newTotal < oldTotal) {
            // Деактивировать лишние места
            List<WorkspaceSeat> allSeats = workspaceSeatRepository.findByWorkspaceIdAndActiveTrueOrderByCode(workspace.getId());
            for (int i = newTotal; i < allSeats.size() && i < oldTotal; i++) {
                WorkspaceSeat seat = allSeats.get(i);
                seat.setActive(false);
                workspaceSeatRepository.save(seat);
            }
        }
    }
}