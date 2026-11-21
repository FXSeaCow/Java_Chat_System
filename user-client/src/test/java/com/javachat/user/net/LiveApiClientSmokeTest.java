package com.javachat.user.net;

import com.javachat.user.mock.MockChatStore;

/** Run against a local server backed by the seeded PostgreSQL test database. */
public final class LiveApiClientSmokeTest {
    private LiveApiClientSmokeTest() { }

    public static void main(String[] args) throws Exception {
        MockChatStore store = MockChatStore.fromSnapshot(new LiveApiClient().snapshot());
        if (store.allUsers().size() != 8 || store.conversationSummaries().size() != 3
                || store.messages(4L).size() != 5 || store.friendList().friends().size() != 2) {
            throw new AssertionError("Unexpected database snapshot");
        }
        System.out.println("LiveApiClientSmokeTest passed");
    }
}
