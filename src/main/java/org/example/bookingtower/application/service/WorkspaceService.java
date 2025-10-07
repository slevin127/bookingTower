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
     * @return созданное рабочее пространство
     */
    @Transactional
    public Workspace createWorkspaceWithSeats(String name, Integer seatsTotal, BigDecimal pricePerHour, Long coworkingId) {
        // Загружаем объект Coworking по его ID
        Optional<Coworking> coworkingOptional = coworkingRepository.findByIdAndActiveTrue(coworkingId);
        if (coworkingOptional.isEmpty()) {
            throw new IllegalArgumentException("Коворкинг с ID " + coworkingId + " не найден или неактивен.");
        }
        Coworking coworking = coworkingOptional.get();

        // Создаем объект рабочего пространства
        Workspace workspace = new Workspace();
        workspace.setName(name);
        workspace.setSeatsTotal(seatsTotal);
        workspace.setPricePerHour(pricePerHour);
        workspace.setActive(true);
        workspace.setCoworking(coworking); // Устанавливаем найденный объект Coworking

        // Сохраняем рабочее пространство
        Workspace savedWorkspace = workspaceRepository.save(workspace);

        // Создаём места (WorkspaceSeat) для рабочего пространства
        List<WorkspaceSeat> seats = new ArrayList<>();
        for (int i = 1; i <= seatsTotal; i++) {
            WorkspaceSeat seat = new WorkspaceSeat();
            seat.setWorkspace(savedWorkspace);
            seat.setCode("Seat-" + i); // Уникальный код для каждого места
            seat.setActive(true);
            seats.add(seat);
        }

        // Сохраняем все места в базу данных
        workspaceSeatRepository.saveAll(seats);

        return savedWorkspace;
    }
}