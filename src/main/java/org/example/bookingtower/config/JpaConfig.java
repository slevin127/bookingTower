package org.example.bookingtower.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Конфигурационный класс JpaConfig приложения BookingTower.
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "org.example.bookingtower.infrastructure.repository")
public class JpaConfig {
}
