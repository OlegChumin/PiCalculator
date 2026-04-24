package org.example.picalculator.pi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrentPiDigitGeneratorTest {

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
