package org.example.picalculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа в приложение визуализации вычисления числа {@code Pi}.
 * <p>
 * Приложение поднимает Spring Boot backend, который:
 * </p>
 * <ul>
 *     <li>отдаёт статический frontend;</li>
 *     <li>генерирует цифры числа {@code Pi};</li>
 *     <li>стримит цифры в браузер через Server-Sent Events.</li>
 * </ul>
 */
@SpringBootApplication
public class PiCalculatorApplication {

    /**
     * Запускает Spring Boot приложение.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PiCalculatorApplication.class);
        application.setHeadless(false);
        application.run(args);
    }

}
