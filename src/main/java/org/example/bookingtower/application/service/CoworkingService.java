package org.example.bookingtower.application.service;

import org.example.bookingtower.domain.entity.Coworking;
import org.example.bookingtower.infrastructure.repository.CoworkingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Сервис для работы с коворкингами.
 */
@Service
public class CoworkingService {

    private final CoworkingRepository coworkingRepository;

    public CoworkingService(CoworkingRepository coworkingRepository) {
        this.coworkingRepository = coworkingRepository;
    }

    /**
     * Возвращает список всех активных коворкингов, отсортированный по названию.
     */
    public List<Coworking> findAllActive() {
        return coworkingRepository.findAllActiveOrderByName();
    }

    /**
     * Возвращает активный коворкинг по идентификатору.
     *
     * @throws NoSuchElementException если коворкинг не найден или отключен
     */
    public Coworking getActiveById(Long id) {
        return coworkingRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NoSuchElementException("Коворкинг не найден или отключен"));
    }
}
