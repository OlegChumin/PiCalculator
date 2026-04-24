package org.example.picalculator.pi;

public record PiDigitEvent(
        long index,
        String symbol,
        long elapsedMillis
) {
}
