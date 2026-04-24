package org.example.picalculator.pi;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
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

    /**
     * Создаёт контроллер потока вычисления {@code Pi}.
     *
     * @param piDigitGenerator генератор цифр числа {@code Pi}
     */
    public PiStreamController(PiDigitGenerator piDigitGenerator) {
        this.piDigitGenerator = piDigitGenerator;
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
        int safeDelayMs = Math.clamp(delayMs, 0, MAX_DELAY_MS);

        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean active = new AtomicBoolean(true);

        emitter.onCompletion(() -> active.set(false));
        emitter.onTimeout(() -> active.set(false));
        emitter.onError(throwable -> active.set(false));

        Thread worker = Thread.ofVirtual().start(() -> streamDigits(emitter, active, safeDigits, safeDelayMs));
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
    private void streamDigits(SseEmitter emitter, AtomicBoolean active, int digits, int delayMs) {
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

                    if (delayMs > 0) {
                        Thread.sleep(delayMs);
                    }
                } catch (IOException | InterruptedException ex) {
                    active.set(false);
                    Thread.currentThread().interrupt();
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
}
