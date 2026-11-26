package com.javachat.server;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.javachat.shared.model.PresenceStatus;

/** Recent authenticated activity for desktop clients on this server. */
public final class PresenceTracker {
    private static final Duration TIMEOUT = Duration.ofSeconds(45);
    private record Session(long userId, Instant lastSeen) { }
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private volatile BiConsumer<Long, PresenceStatus> listener = (id, status) -> { };

    public void setListener(BiConsumer<Long, PresenceStatus> listener) {
        this.listener = listener;
    }

    public void online(long userId, String token) {
        boolean becameOnline;
        synchronized (sessions) {
            becameOnline = sessions.values().stream().noneMatch(session -> session.userId() == userId);
            sessions.put(token, new Session(userId, Instant.now()));
        }
        if (becameOnline) listener.accept(userId, PresenceStatus.ONLINE);
    }

    public void offline(String token) {
        Long becameOffline = null;
        synchronized (sessions) {
            Session removed = sessions.remove(token);
            if (removed != null && sessions.values().stream()
                    .noneMatch(session -> session.userId() == removed.userId())) {
                becameOffline = removed.userId();
            }
        }
        if (becameOffline != null) listener.accept(becameOffline, PresenceStatus.OFFLINE);
    }

    public Set<Long> onlineUserIds() {
        Instant cutoff = Instant.now().minus(TIMEOUT);
        Set<Long> expired = new HashSet<>();
        Set<Long> online;
        synchronized (sessions) {
            sessions.entrySet().removeIf(entry -> {
                if (entry.getValue().lastSeen().isBefore(cutoff)) {
                    expired.add(entry.getValue().userId());
                    return true;
                }
                return false;
            });
            online = sessions.values().stream().map(Session::userId).collect(Collectors.toUnmodifiableSet());
        }
        expired.stream().filter(id -> !online.contains(id))
                .forEach(id -> listener.accept(id, PresenceStatus.OFFLINE));
        return online;
    }
}
