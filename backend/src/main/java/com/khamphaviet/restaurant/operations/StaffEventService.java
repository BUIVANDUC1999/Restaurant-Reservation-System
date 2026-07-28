package com.khamphaviet.restaurant.operations;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class StaffEventService {
    private final CopyOnWriteArrayList<SseEmitter> clients = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        clients.add(emitter);
        emitter.onCompletion(() -> clients.remove(emitter));
        emitter.onTimeout(() -> clients.remove(emitter));
        emitter.onError(error -> clients.remove(emitter));
        send(emitter, "CONNECTED", "Đã kết nối realtime", "Màn hình sẽ tự cập nhật khi có thay đổi.", null);
        return emitter;
    }

    public void publish(String type, String title, String message, Long referenceId) {
        clients.forEach(client -> send(client, type, title, message, referenceId));
    }

    @Scheduled(fixedDelay = 20000)
    public void heartbeat() {
        clients.forEach(client -> {
            try { client.send(SseEmitter.event().comment("heartbeat")); }
            catch (IOException error) { clients.remove(client); }
        });
    }

    private void send(SseEmitter emitter, String type, String title, String message, Long referenceId) {
        try {
            emitter.send(SseEmitter.event().name("operational")
                    .data(new StaffEvent(type, title, message, referenceId, Instant.now())));
        } catch (IOException error) {
            clients.remove(emitter);
        }
    }

    public record StaffEvent(String type, String title, String message, Long referenceId, Instant createdAt) {}
}
