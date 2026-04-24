package org.example.picalculator.pi;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * REST-контроллер, который стримит цифры числа {@code Pi} на frontend через SSE.
 * <p>
 * Каждый символ отправляется отдельным событием с накопленным временем вычисления.
 * Это позволяет интерфейсу показывать большую цифровую ленту в реальном времени.
 * </p>
 */
@RestController
public class PiStreamController {

    private static final int DEFAULT_DIGITS = 300;
    private static final int MAX_DIGITS = 2_000;
    private static final int DEFAULT_DELAY_MS = 35;
    private static final int MAX_DELAY_MS = 500;

    private final PiDigitGenerator piDigitGenerator;
    private final ConcurrentPiDigitGenerator concurrentPiDigitGenerator;

    /**
     * Создаёт контроллер потока вычисления {@code Pi}.
     *
     * @param piDigitGenerator генератор цифр числа {@code Pi}
     */
    public PiStreamController(PiDigitGenerator piDigitGenerator, ConcurrentPiDigitGenerator concurrentPiDigitGenerator) {
        this.piDigitGenerator = piDigitGenerator;
        this.concurrentPiDigitGenerator = concurrentPiDigitGenerator;
    }

    /**
     * Открывает SSE-поток и начинает по одной отправлять цифры числа {@code Pi}.
     *
     * @param digits сколько цифр после запятой запрашивает клиент
     * @param delayMs задержка между отправками соседних символов в миллисекундах
     * @return SSE-эмиттер для передачи цифр на frontend
     */
    @GetMapping(path = "/api/pi/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPi(
            @RequestParam(defaultValue = "" + DEFAULT_DIGITS) int digits,
            @RequestParam(defaultValue = "" + DEFAULT_DELAY_MS) int delayMs
    ) {
        int safeDigits = Math.clamp(digits, 1, MAX_DIGITS);

        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean active = new AtomicBoolean(true);

        emitter.onCompletion(() -> active.set(false));
        emitter.onTimeout(() -> active.set(false));
        emitter.onError(throwable -> active.set(false));

        Thread worker = Thread.ofVirtual().start(() -> streamDigits(emitter, active, safeDigits));
        emitter.onCompletion(worker::interrupt);
        emitter.onTimeout(worker::interrupt);

        return emitter;
    }

    /**
     * Открывает поток сравнения однопоточного и многопоточного режимов.
     *
     * @param digits сколько цифр после запятой запрашивает клиент
     * @param delayMs задержка между соседними индексами при визуализации
     * @return SSE-эмиттер для сравнительного показа двух строк
     */
    @GetMapping(path = "/api/pi/compare-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter comparePi(
            @RequestParam(defaultValue = "" + DEFAULT_DIGITS) int digits,
            @RequestParam(defaultValue = "" + DEFAULT_DELAY_MS) int delayMs
    ) {
        int safeDigits = Math.clamp(digits, 1, MAX_DIGITS);

        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean active = new AtomicBoolean(true);

        emitter.onCompletion(() -> active.set(false));
        emitter.onTimeout(() -> active.set(false));
        emitter.onError(throwable -> active.set(false));

        Thread worker = Thread.ofVirtual().start(() -> streamComparison(emitter, active, safeDigits));
        emitter.onCompletion(worker::interrupt);
        emitter.onTimeout(worker::interrupt);

        return emitter;
    }

    /**
     * Выполняет вычисление и отправляет клиенту цифры числа {@code Pi}.
     *
     * @param emitter SSE-канал до браузера
     * @param active флаг активности соединения
     * @param digits количество цифр после запятой
     * @param delayMs задержка между событиями
     */
    private void streamDigits(SseEmitter emitter, AtomicBoolean active, int digits) {
        long startedAt = System.nanoTime();
        AtomicLong index = new AtomicLong();

        try {
            piDigitGenerator.generateDigits(digits, symbol -> {
                if (!active.get()) {
                    return;
                }

                long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
                PiDigitEvent event = new PiDigitEvent(index.getAndIncrement(), Character.toString(symbol), elapsedMillis);

                try {
                    emitter.send(SseEmitter.event().name("digit").data(event));
                } catch (IOException ex) {
                    active.set(false);
                }
            });

            if (active.get()) {
                emitter.send(SseEmitter.event().name("done").data("completed"));
            }
            emitter.complete();
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }

    private void streamComparison(SseEmitter emitter, AtomicBoolean active, int digits) {
        try {
            CompletableFuture<PiComputationResult> sequentialFuture = CompletableFuture.supplyAsync(
                    () -> concurrentPiDigitGenerator.generateDigits(digits, 1)
            );
            CompletableFuture<PiComputationResult> concurrentFuture = CompletableFuture.supplyAsync(
                    () -> concurrentPiDigitGenerator.generateDigits(digits)
            );

            PiComputationResult singleThreadResult = sequentialFuture.join();
            PiComputationResult concurrentResult = concurrentFuture.join();

            PiComparisonPayload payload = new PiComparisonPayload(
                    new PiCompareMetadata(
                            concurrentResult.processorCount(),
                            concurrentResult.threadCount()
                    ),
                    singleThreadResult.events(),
                    concurrentResult.events()
            );

            emitter.send(SseEmitter.event().name("snapshot").data(payload));

            if (active.get()) {
                emitter.send(SseEmitter.event().name("done").data("completed"));
            }
            emitter.complete();
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }

}
