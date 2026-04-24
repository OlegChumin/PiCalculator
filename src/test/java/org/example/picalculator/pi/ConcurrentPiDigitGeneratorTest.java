package org.example.picalculator.pi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверки генератора сравнения для режимов {@code 1 поток} и {@code N потоков}.
 */
class ConcurrentPiDigitGeneratorTest {

    /**
     * Проверяет корректный префикс числа {@code Pi} при автоматическом выборе числа потоков.
     */
    @Test
    void generatesKnownPiPrefixForConcurrentMode() {
        ConcurrentPiDigitGenerator generator = new ConcurrentPiDigitGenerator();

        PiComputationResult result = generator.generateDigits(20);

        StringBuilder builder = new StringBuilder();
        for (PiDigitEvent event : result.events()) {
            builder.append(event.symbol());
        }

        assertThat(builder.toString()).isEqualTo("3.14159265358979323846");
        assertThat(result.threadCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.processorCount()).isGreaterThanOrEqualTo(1);
    }

    /**
     * Проверяет корректный префикс числа {@code Pi} при явном запуске в одном потоке.
     */
    @Test
    void generatesKnownPiPrefixForSingleThreadMode() {
        ConcurrentPiDigitGenerator generator = new ConcurrentPiDigitGenerator();

        PiComputationResult result = generator.generateDigits(20, 1);

        StringBuilder builder = new StringBuilder();
        for (PiDigitEvent event : result.events()) {
            builder.append(event.symbol());
        }

        assertThat(builder.toString()).isEqualTo("3.14159265358979323846");
        assertThat(result.threadCount()).isEqualTo(1);
        assertThat(result.processorCount()).isGreaterThanOrEqualTo(1);
    }
}
