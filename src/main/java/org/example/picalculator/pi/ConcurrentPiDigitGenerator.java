package org.example.picalculator.pi;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Вычисляет число {@code Pi} по формуле Машина в настраиваемом числе потоков.
 * <p>
 * Здесь используется формула Машина с независимым расчётом диапазонов ряда для
 * {@code arctan(1 / 5)}. Один и тот же алгоритм может выполняться как в одном
 * потоке, так и в нескольких, что позволяет сравнивать режимы исполнения без
 * подмены вычислительной схемы.
 * </p>
 */
@Component
public class ConcurrentPiDigitGenerator {

    private static final int EXTRA_PRECISION = 12;
    private static final BigDecimal SIXTEEN = BigDecimal.valueOf(16);
    private static final BigDecimal FOUR = BigDecimal.valueOf(4);

    /**
     * Вычисляет события символов числа {@code Pi} с автоматически выбранным числом потоков.
     *
     * @param digitsAfterDecimal сколько цифр после запятой нужно получить
     * @return список событий и параметры окружения вычисления
     */
    public PiComputationResult generateDigits(int digitsAfterDecimal) {
        int processors = Runtime.getRuntime().availableProcessors();
        return generateDigits(digitsAfterDecimal, Math.max(1, Math.min(processors, 8)));
    }

    /**
     * Вычисляет события символов числа {@code Pi} в указанном числе потоков.
     *
     * @param digitsAfterDecimal сколько цифр после запятой нужно получить
     * @param requestedThreadCount желаемое число потоков
     * @return список событий и параметры окружения вычисления
     */
    public PiComputationResult generateDigits(int digitsAfterDecimal, int requestedThreadCount) {
        if (digitsAfterDecimal < 0) {
            throw new IllegalArgumentException("digitsAfterDecimal must be >= 0");
        }

        int processors = Runtime.getRuntime().availableProcessors();
        int threadCount = Math.max(1, Math.min(requestedThreadCount, Math.max(1, processors)));
        int precision = digitsAfterDecimal + EXTRA_PRECISION;
        MathContext mathContext = new MathContext(precision, RoundingMode.HALF_EVEN);

        long startedAt = System.nanoTime();
        int arctan239Terms = estimateTermCount(239, digitsAfterDecimal);
        BigDecimal arctan239 = computeArctanSequential(239, arctan239Terms, mathContext);
        long arctan239CompletedAt = System.nanoTime();

        int arctan5Terms = estimateTermCount(5, digitsAfterDecimal);
        int chunkCount = Math.max(1, Math.min(threadCount * 4, arctan5Terms));
        List<ChunkRange> ranges = splitIntoRanges(arctan5Terms, chunkCount);

        List<ChunkResult> chunkResults = computeArctanChunks(5, ranges, mathContext, threadCount);

        BigDecimal fullArctan5 = BigDecimal.ZERO;
        long finalReadyAt = arctan239CompletedAt;
        for (ChunkResult chunkResult : chunkResults) {
            fullArctan5 = fullArctan5.add(chunkResult.sum(), mathContext);
            finalReadyAt = Math.max(finalReadyAt, chunkResult.completedAtNanos());
        }

        String finalPi = formatPi(fullArctan5.multiply(SIXTEEN, mathContext)
                .subtract(arctan239.multiply(FOUR, mathContext), mathContext), digitsAfterDecimal);

        List<PiDigitEvent> events = buildTimedEvents(chunkResults, arctan239, arctan239CompletedAt, finalPi, mathContext, startedAt);
        if (events.size() < finalPi.length()) {
            long elapsedMillis = Math.max(0L, (finalReadyAt - startedAt) / 1_000_000);
            for (int i = events.size(); i < finalPi.length(); i++) {
                events.add(new PiDigitEvent(i, Character.toString(finalPi.charAt(i)), elapsedMillis));
            }
        }

        return new PiComputationResult(events, threadCount, processors);
    }

    private List<PiDigitEvent> buildTimedEvents(
            List<ChunkResult> chunkResults,
            BigDecimal arctan239,
            long arctan239CompletedAt,
            String finalPi,
            MathContext mathContext,
            long startedAt
    ) {
        List<PiDigitEvent> events = new ArrayList<>(finalPi.length());
        BigDecimal partialArctan5 = BigDecimal.ZERO;
        int previousStableLength = 0;
        long readyAt = arctan239CompletedAt;

        for (ChunkResult chunkResult : chunkResults) {
            partialArctan5 = partialArctan5.add(chunkResult.sum(), mathContext);
            readyAt = Math.max(readyAt, chunkResult.completedAtNanos());

            String currentPi = formatPi(partialArctan5.multiply(SIXTEEN, mathContext)
                    .subtract(arctan239.multiply(FOUR, mathContext), mathContext), finalPi.length() - 2);

            int stableLength = commonPrefixLength(currentPi, finalPi);
            long elapsedMillis = Math.max(0L, (readyAt - startedAt) / 1_000_000);

            for (int i = previousStableLength; i < stableLength; i++) {
                events.add(new PiDigitEvent(i, Character.toString(finalPi.charAt(i)), elapsedMillis));
            }
            previousStableLength = stableLength;
        }

        return events;
    }

    private List<ChunkResult> computeArctanChunks(int x, List<ChunkRange> ranges, MathContext mathContext, int threadCount) {
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, Math.min(ranges.size(), threadCount)));
        try {
            List<CompletableFuture<ChunkResult>> futures = new ArrayList<>(ranges.size());
            for (ChunkRange range : ranges) {
                futures.add(CompletableFuture.supplyAsync(() -> computeArctanChunk(x, range, mathContext), executor));
            }

            List<ChunkResult> results = new ArrayList<>(ranges.size());
            for (CompletableFuture<ChunkResult> future : futures) {
                results.add(future.join());
            }
            results.sort((left, right) -> Integer.compare(left.startTerm(), right.startTerm()));
            return results;
        } finally {
            executor.shutdown();
        }
    }

    private ChunkResult computeArctanChunk(int x, ChunkRange range, MathContext mathContext) {
        BigDecimal xValue = BigDecimal.valueOf(x);
        BigDecimal xSquared = BigDecimal.valueOf((long) x * x);
        BigDecimal reciprocalPower = BigDecimal.ONE.divide(xValue.pow(range.startTerm() * 2 + 1, mathContext), mathContext);
        BigDecimal sum = BigDecimal.ZERO;

        for (int termIndex = range.startTerm(); termIndex < range.endTerm(); termIndex++) {
            BigDecimal denominator = BigDecimal.valueOf(termIndex * 2L + 1);
            BigDecimal term = reciprocalPower.divide(denominator, mathContext);
            sum = (termIndex % 2 == 0)
                    ? sum.add(term, mathContext)
                    : sum.subtract(term, mathContext);

            reciprocalPower = reciprocalPower.divide(xSquared, mathContext);
        }

        return new ChunkResult(range.startTerm(), sum, System.nanoTime());
    }

    private BigDecimal computeArctanSequential(int x, int terms, MathContext mathContext) {
        BigDecimal xValue = BigDecimal.valueOf(x);
        BigDecimal xSquared = BigDecimal.valueOf((long) x * x);
        BigDecimal reciprocalPower = BigDecimal.ONE.divide(xValue, mathContext);
        BigDecimal sum = BigDecimal.ZERO;

        for (int termIndex = 0; termIndex < terms; termIndex++) {
            BigDecimal denominator = BigDecimal.valueOf(termIndex * 2L + 1);
            BigDecimal term = reciprocalPower.divide(denominator, mathContext);
            sum = (termIndex % 2 == 0)
                    ? sum.add(term, mathContext)
                    : sum.subtract(term, mathContext);

            reciprocalPower = reciprocalPower.divide(xSquared, mathContext);
        }

        return sum;
    }

    private int estimateTermCount(int x, int digitsAfterDecimal) {
        double terms = (digitsAfterDecimal + EXTRA_PRECISION) / (2.0 * Math.log10(x));
        return Math.max(4, (int) Math.ceil(terms) + 4);
    }

    private List<ChunkRange> splitIntoRanges(int totalTerms, int chunkCount) {
        List<ChunkRange> ranges = new ArrayList<>(chunkCount);
        int baseChunkSize = totalTerms / chunkCount;
        int remainder = totalTerms % chunkCount;
        int start = 0;

        for (int i = 0; i < chunkCount; i++) {
            int size = baseChunkSize + (i < remainder ? 1 : 0);
            int end = start + size;
            if (start < end) {
                ranges.add(new ChunkRange(start, end));
            }
            start = end;
        }

        return ranges;
    }

    private String formatPi(BigDecimal value, int digitsAfterDecimal) {
        return value.setScale(digitsAfterDecimal, RoundingMode.DOWN).toPlainString();
    }

    private int commonPrefixLength(String left, String right) {
        int limit = Math.min(left.length(), right.length());
        int index = 0;
        while (index < limit && left.charAt(index) == right.charAt(index)) {
            index++;
        }
        return index;
    }

    private record ChunkRange(int startTerm, int endTerm) {
    }

    private record ChunkResult(int startTerm, BigDecimal sum, long completedAtNanos) {
    }
}
