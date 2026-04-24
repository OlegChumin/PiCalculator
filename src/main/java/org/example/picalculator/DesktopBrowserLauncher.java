package org.example.picalculator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

/**
 * Открывает браузер на локальной странице приложения после успешного старта backend.
 * <p>
 * Используется только для локального Windows launcher и не обязателен для Docker-сценария.
 * Поведение включается настройкой {@code pi.browser.auto-open=true}.
 * </p>
 */
@Component
public class DesktopBrowserLauncher {

    private final boolean autoOpenEnabled;
    private final String applicationUrl;

    /**
     * Создаёт компонент для автоматического открытия браузера.
     *
     * @param autoOpenEnabled нужно ли автоматически открывать страницу
     * @param applicationUrl URL, который следует открыть после старта
     */
    public DesktopBrowserLauncher(
            @Value("${pi.browser.auto-open:false}") boolean autoOpenEnabled,
            @Value("${pi.browser.auto-open-url:http://localhost:8181/}") String applicationUrl
    ) {
        this.autoOpenEnabled = autoOpenEnabled;
        this.applicationUrl = applicationUrl;
    }

    /**
     * Пытается открыть браузер после полной готовности Spring Boot приложения.
     *
     * @param event событие готовности приложения
     */
    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser(ApplicationReadyEvent event) {
        if (!autoOpenEnabled || !Desktop.isDesktopSupported()) {
            return;
        }

        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(750);
                Desktop.getDesktop().browse(URI.create(applicationUrl));
            } catch (IOException | InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
