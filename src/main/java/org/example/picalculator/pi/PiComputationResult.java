package org.example.picalculator.pi;

import java.util.List;

/**
 * Результат вычисления с метаданными по режиму исполнения.
 *
 * @param events события символов числа {@code Pi}
 * @param threadCount число потоков, использованных вычислением
 * @param processorCount число логических процессоров машины
 */
public record PiComputationResult(
        List<PiDigitEvent> events,
        int threadCount,
        int processorCount
) {
}
