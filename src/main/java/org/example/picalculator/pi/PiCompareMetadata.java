package org.example.picalculator.pi;

/**
 * Метаданные сравнительного режима вычисления {@code Pi}.
 *
 * @param processors число логических процессоров текущей машины
 * @param concurrentThreads число потоков, использованных для многопоточного расчёта
 */
public record PiCompareMetadata(
        int processors,
        int concurrentThreads
) {
}
