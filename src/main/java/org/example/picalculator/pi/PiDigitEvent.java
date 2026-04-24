package org.example.picalculator.pi;

/**
 * Событие с очередным символом числа {@code Pi}, отправляемое на frontend.
 *
 * @param index порядковый номер символа в потоке
 * @param symbol символ числа {@code Pi}, например {@code 3}, {@code .} или {@code 1}
 * @param elapsedMillis время в миллисекундах с начала вычисления до появления символа
 */
public record PiDigitEvent(
        long index,
        String symbol,
        long elapsedMillis
) {
}
