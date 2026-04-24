package org.example.picalculator.pi;

import java.util.List;

/**
 * Полный снимок сравнения однопоточного и многопоточного вычисления {@code Pi}.
 *
 * @param metadata параметры машины и многопоточного режима
 * @param singleThreadEvents события однопоточного расчёта
 * @param concurrentEvents события многопоточного расчёта
 */
public record PiComparisonPayload(
        PiCompareMetadata metadata,
        List<PiDigitEvent> singleThreadEvents,
        List<PiDigitEvent> concurrentEvents
) {
}
