package org.example.bookingtower;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Главный класс приложения BookingTower, запускающий Spring Boot.
 */
@SpringBootApplication
public class BookingTowerApplication {

    /**
     * Точка входа в приложение.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(BookingTowerApplication.class, args);
    }

}
