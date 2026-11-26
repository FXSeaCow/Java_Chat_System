package com.javachat.server;

import java.util.ArrayList;
import java.util.List;

/** Checks that one logout does not hide another active session. */
public final class PresenceTrackerSmokeTest {
    private PresenceTrackerSmokeTest() { }

    public static void main(String[] args) {
        PresenceTracker tracker = new PresenceTracker();
        List<String> events = new ArrayList<>();
        tracker.setListener((id, status) -> events.add(id + ":" + status));
        tracker.online(10L, "first");
        tracker.online(10L, "second");
        tracker.online(20L, "third");
        require(tracker.onlineUserIds().size() == 2, "Users were not deduplicated");
        tracker.offline("first");
        require(tracker.onlineUserIds().contains(10L), "Another session was still online");
        tracker.offline("second");
        require(!tracker.onlineUserIds().contains(10L), "Logged out user stayed online");
        require(events.equals(List.of("10:ONLINE", "20:ONLINE", "10:OFFLINE")),
                "Presence transitions were not emitted once per user");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
