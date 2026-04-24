package org.example.picalculator.pi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты генератора цифр числа {@code Pi}.
 */
class PiDigitGeneratorTest {

    /**
     * Проверяет, что генератор выдаёт корректный префикс числа {@code Pi}.
     */
    @Test
    void generatesKnownPiPrefix() {
        PiDigitGenerator generator = new PiDigitGenerator();
        StringBuilder builder = new StringBuilder();

        generator.generateDigits(20, builder::append);

        assertThat(builder.toString()).isEqualTo("3.14159265358979323846");
    }
}
