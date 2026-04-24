package org.example.picalculator.pi;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Генерирует цифры числа {@code Pi} по одной.
 * <p>
 * Используется spigot-алгоритм для десятичного разложения {@code Pi}.
 * Алгоритм позволяет последовательно получать цифры без вычисления всего
 * числа целиком в виде строки заранее.
 * </p>
 */
@Component
public class PiDigitGenerator {

    /**
     * Генерирует символы числа {@code Pi} и передаёт их потребителю по мере вычисления.
     * <p>
     * Первый выведенный символом будет целая часть {@code 3}, сразу после неё
     * автоматически добавляется десятичная точка {@code .}. Затем передаются
     * цифры после запятой.
     * </p>
     *
     * @param digitsAfterDecimal сколько цифр после запятой нужно вычислить
     * @param consumer обработчик очередного вычисленного символа
     * @throws IllegalArgumentException если {@code digitsAfterDecimal < 0}
     */
    public void generateDigits(int digitsAfterDecimal, Consumer<Character> consumer) {
        if (digitsAfterDecimal < 0) {
            throw new IllegalArgumentException("digitsAfterDecimal must be >= 0");
        }

        int totalDigits = digitsAfterDecimal + 1;
        int boxes = totalDigits * 10 / 3 + 1;
        int[] reminders = new int[boxes];
        Arrays.fill(reminders, 2);

        int heldNines = 0;
        int predigit = 0;
        boolean firstDigitEmitted = false;
        boolean skipLeadingZero = true;

        for (int i = 0; i < totalDigits; i++) {
            int q = 0;

            for (int j = boxes; j > 0; j--) {
                int numerator = reminders[j - 1] * 10 + q * j;
                int denominator = 2 * j - 1;

                reminders[j - 1] = numerator % denominator;
                q = numerator / denominator;
            }

            reminders[0] = q % 10;
            q /= 10;

            if (q == 9) {
                heldNines++;
                continue;
            }

            if (q == 10) {
                boolean emitted = emitDigit(predigit + 1, firstDigitEmitted, consumer, skipLeadingZero);
                firstDigitEmitted = firstDigitEmitted || emitted;
                skipLeadingZero = skipLeadingZero && !emitted;
                for (int k = 0; k < heldNines; k++) {
                    emitted = emitDigit(0, firstDigitEmitted, consumer, skipLeadingZero);
                    firstDigitEmitted = firstDigitEmitted || emitted;
                    skipLeadingZero = skipLeadingZero && !emitted;
                }
                predigit = 0;
                heldNines = 0;
                continue;
            }

            boolean emitted = emitDigit(predigit, firstDigitEmitted, consumer, skipLeadingZero);
            firstDigitEmitted = firstDigitEmitted || emitted;
            skipLeadingZero = skipLeadingZero && !emitted;
            predigit = q;

            for (int k = 0; k < heldNines; k++) {
                emitted = emitDigit(9, firstDigitEmitted, consumer, skipLeadingZero);
                firstDigitEmitted = firstDigitEmitted || emitted;
                skipLeadingZero = skipLeadingZero && !emitted;
            }
            heldNines = 0;
        }

        emitDigit(predigit, firstDigitEmitted, consumer, skipLeadingZero);
    }

    /**
     * Передаёт очередную цифру потребителю и при первом выводе добавляет десятичную точку.
     *
     * @param digit вычисленная цифра
     * @param firstDigitEmitted была ли уже выведена первая значащая цифра
     * @param consumer получатель символа
     * @param skipLeadingZero нужно ли пропускать начальный служебный ноль алгоритма
     * @return {@code true}, если цифра действительно была выведена
     */
    private boolean emitDigit(int digit, boolean firstDigitEmitted, Consumer<Character> consumer, boolean skipLeadingZero) {
        if (skipLeadingZero && digit == 0) {
            return false;
        }

        consumer.accept((char) ('0' + digit));

        if (!firstDigitEmitted) {
            consumer.accept('.');
        }

        return true;
    }
}
