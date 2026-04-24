package org.example.picalculator.pi;

/**
 * Событие символа для сравнительного показа нескольких режимов вычисления {@code Pi}.
 *
 * @param row идентификатор строки сравнения
 * @param index порядковый номер символа
 * @param symbol символ числа {@code Pi}
 * @param elapsedMillis время от старта вычисления до готовности символа
 */
public record PiCompareDigitEvent(
        String row,
        long index,
        String symbol,
        long elapsedMillis
) {
}
